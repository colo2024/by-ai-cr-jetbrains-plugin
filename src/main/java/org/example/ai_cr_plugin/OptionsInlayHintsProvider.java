package org.example.ai_cr_plugin;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class OptionsInlayHintsProvider implements InlayHintsProvider<NoSettings> {

  public OptionsInlayHintsProvider() {
//    System.out.println("OptionsInlayHintsProvider initialized");
  }

  @Override
  public boolean isVisibleInSettings() {
    System.out.print("isVisibleInSettings\n");
    return true;
  }

  @Override
  @Nullable
  public String getPreviewText() {
    return """
      public class Example {
          public void sampleMethod() {
              // Method implementation
          }
      }
      """;
  }

  @NotNull
  @Override
  public SettingsKey<NoSettings> getKey() {
    return new SettingsKey<>("org.example.ai_cr_plugin.inlay.hints");
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getName() {
    return "By AI CR plugin inlay hints";
  }

  @NotNull
  @Override
  public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
    return new ImmediateConfigurable() {
      @NotNull
      @Override
      public JComponent createComponent(@NotNull ChangeListener listener) {
        return new JPanel();
      }
    };
  }

  @NotNull
  @Override
  public NoSettings createSettings() {
    return new NoSettings();
  }

  @Override
  public boolean isLanguageSupported(@NotNull Language language) {
    System.out.print("isLanguageSupported: " + language.getID() + "\n");
    return switch (language.getID()) {
      case "JAVA", "Python", "go", "ObjectiveC", "JavaScript", "TypeScript", "ECMAScript 6", "TypeScript JSX", "Dart" ->
        true;
      default -> false;
    };
  }


  @NotNull
  @Override
  public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                             @NotNull Editor editor,
                                             @NotNull NoSettings settings,
                                             @NotNull InlayHintsSink sink) {

    editor.getComponent().revalidate();

    return new FactoryInlayHintsCollector(editor) {
      @Override
      public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
        System.out.print("collect");
        ApplicationManager.getApplication().invokeLater(() -> {
          if (ElementUtils.isMethodOrFunction(element)) {
            Document document = editor.getDocument();
            int methodStartOffset = element.getTextRange().getStartOffset();
            int lineNumber = document.getLineNumber(methodStartOffset);
            int lineStartOffset = document.getLineStartOffset(lineNumber);
            String lineText = document.getText().substring(lineStartOffset, methodStartOffset);
            int indentSpaces = (lineText.length() - lineText.stripLeading().length()) * 2;

            //  System.out.print("Method: " + method.getName() + " at line: " + lineNumber + " with indent: " + indentSpaces + "\n");

            // 创建可点击的提示块
            PresentationFactory factory = getFactory();

            InlayPresentation explainButton = createActionButton(factory, ActionType.EXPLAIN, element);
            InlayPresentation improveButton = createActionButton(factory, ActionType.IMPROVE, element);
            InlayPresentation docButton = createActionButton(factory, ActionType.DOCSTRING, element);
            InlayPresentation testButton = createActionButton(factory, ActionType.TEST, element);

            sink.addBlockElement(
              lineStartOffset,
              true,
              true,
              0,
              factory.seq(
                factory.seq(
                  factory.smallText(" ".repeat(indentSpaces))
                ),
                factory.smallText("By AI CR: Options -"),
                explainButton,
                docButton,
                improveButton,
                testButton
              )
            );
          }
        });

        return true;
      }

    };
  }

  private InlayPresentation createActionButton(
    PresentationFactory factory,
    ActionType actionType,
    PsiElement element) {
    return factory.referenceOnHover(
      factory.seq(
        factory.smallText(actionType.getInlayText())
      ),
      (event, point) -> executeCommand(actionType.getActionCode(), element)
    );
  }

  private void executeCommand(String command, PsiElement element) {
    Project project = element.getProject();

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

      String fnCode = Utils.getEscapedCode(element.getText());

      String jsOriginalCode = """
          sendActionWithCode({
            fnCode: "{fnCode}",
            fnName: "{fnName}",
            filePath: "{filePath}",
            fileType: "{fileType}",
            action: "{action}"
          });
        """;

      String jsCode = jsOriginalCode.replace("{fnCode}", fnCode)
        .replace("{fnName}", ElementUtils.getMethodName(element))
        .replace("{filePath}", ElementUtils.getFilePath(element))
        .replace("{fileType}", Utils.getFileLanguage(element.getContainingFile().getVirtualFile()))
        .replace("{action}", command);

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
