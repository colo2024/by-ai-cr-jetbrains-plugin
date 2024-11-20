package org.example.ai_cr_plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.util.Objects;

public class EditorPopupCodeAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();

    String actionType = e.getActionManager()
      .getAction(Objects.requireNonNull(e.getActionManager().getId(this)))
      .getTemplateText();

    ActionType type = ActionType.fromEditorMenuText(actionType);
    sendAction(project, type.getActionCode());
  }

  private void sendAction(Project project, String action ) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow("ToolwindowPanel");

    if (toolWindow == null) {
      Messages.showMessageDialog("Tool window is null, restart the IDE and try again", "Error", Messages.getErrorIcon());
      return;
    }

    toolWindow.show(() -> {
      ToolWindowPanelFactory.CRToolWindowContent toolWindowContent = ToolWindowPanelFactory.getToolWindowContent();
      if (toolWindowContent == null) {
        Messages.showMessageDialog("Plugin hasn't been initiated, wait second and please try again or restart the IDE.", "Error", Messages.getErrorIcon());
        return;
      }

      Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

      if (editor == null) {
        Messages.showMessageDialog("No editor is selected", "Error", Messages.getErrorIcon());
        return;
      }

      String code2Enhance = "";

      SelectionModel selectionModel = editor.getSelectionModel();
      String selectedText = selectionModel.getSelectedText();

      if (selectedText == null || selectedText.trim().isEmpty()) {
        code2Enhance = editor.getDocument().getText();
      } else {
        code2Enhance = selectedText;
      }

      code2Enhance = Utils.getEscapedCode(code2Enhance);


      String jsOriginalCode = """
          sendActionWithCode({
            fnCode: "{fnCode}",
            filePath: "{filePath}",
            fileType: "{fileType}",
            action: "{action}"
          });
        """;

      VirtualFile file = editor.getVirtualFile();

      String jsCode = jsOriginalCode.replace("{fnCode}", code2Enhance)
        .replace("{fileType}", Utils.getFileLanguage(editor.getVirtualFile()))
        .replace("{filePath}", file.getPath())
        .replace("{action}", action);

      JBCefBrowser browser = toolWindowContent.getBrowser();

      if (toolWindowContent.isBrowserReady()) {
        browser.getCefBrowser().executeJavaScript(jsCode, browser.getCefBrowser().getURL(), 0);
      } else {
        Timer timer = new Timer(Constants.delay, e -> {
          browser.getCefBrowser().executeJavaScript(jsCode, browser.getCefBrowser().getURL(), 0);
        });
        timer.setRepeats(false);  // 只执行一次
        timer.start();
      }
    });

  }
}
