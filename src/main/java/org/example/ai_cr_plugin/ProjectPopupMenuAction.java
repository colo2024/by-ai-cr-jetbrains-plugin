package org.example.ai_cr_plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.jcef.JBCefBrowser;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class ProjectPopupMenuAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

    String actionType = e.getActionManager()
      .getAction(Objects.requireNonNull(e.getActionManager().getId(this)))
      .getTemplateText();


    ActionType type = ActionType.fromProjectMenuText(actionType);
    sendAction(project, selectedFile, type.getActionCode());

  }

  private void sendAction(Project project, VirtualFile file, String action) {
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


      JBCefBrowser browser = toolWindowContent.getBrowser();

      String code2Enhance;
      try {
        code2Enhance = new String(file.contentsToByteArray(), file.getCharset());
      } catch (IOException ex) {
        Messages.showMessageDialog("Failed to read file content: " + ex.getMessage(), "Error", Messages.getErrorIcon());
        return;
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

      String jsCode = jsOriginalCode.replace("{fnCode}", code2Enhance)
        .replace("{fileType}", Utils.getFileLanguage(file))
        .replace("{filePath}", file.getPath())
        .replace("{action}", action);

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
