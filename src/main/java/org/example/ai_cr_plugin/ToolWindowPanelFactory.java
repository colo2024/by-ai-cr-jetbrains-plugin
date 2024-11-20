package org.example.ai_cr_plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.*;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.example.ai_cr_plugin.settings.SwitchEnvironmentAction;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.google.gson.Gson;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import com.intellij.openapi.diagnostic.Logger;

public class ToolWindowPanelFactory implements ToolWindowFactory, DumbAware {
  static {
    System.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, "1000");
  }

  private static final Logger LOG = Logger.getInstance(ToolWindowPanelFactory.class);

  private static CRToolWindowContent toolWindowContent;

  private static Project projectInstance;

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    projectInstance = project;

    // add listeners for project
    addProjectListener(project);

    toolWindowContent = new CRToolWindowContent(toolWindow);
    Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "By AI CR Plugin", true);
    toolWindow.getContentManager().addContent(content);

  }

  public static CRToolWindowContent getToolWindowContent() {
    return toolWindowContent;
  }

  public static class CRToolWindowContent {
    private final JPanel contentPanel = new JPanel();
    private final JBCefBrowser browser;
    private static boolean isBrowserReady = false;

    private JBCefJSQuery askFunctionQuery;
    private JBCefJSQuery diffQuery;
    private JBCefJSQuery askFileQuery;
    private JBCefJSQuery openFileQuery;

    // 标记是否已经初始化过 JSQuery
    private boolean jsQueriesInitialized = false;

    private void initJSQueries() {
      if (!jsQueriesInitialized) {
        askFunctionQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);
        diffQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);
        askFileQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);
        openFileQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);
        jsQueriesInitialized = true;
      }
    }

    public CRToolWindowContent(ToolWindow toolWindow) {
      // 添加按钮到标题栏
      toolWindow.setTitleActions(Arrays.asList(new AnAction("Refresh", "Refresh content", AllIcons.Actions.Refresh) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          loadPage();
        }
      }));


      browser = JBCefBrowser.createBuilder().setOffScreenRendering(false)  // 禁用离屏渲染
        .build();

      contentPanel.setLayout(new BorderLayout());
      contentPanel.add(browser.getComponent(), BorderLayout.CENTER);

      loadPage();
    }

    public JPanel getContentPanel() {
      return contentPanel;
    }

    // Getter for browser ready state
    public boolean isBrowserReady() {
      return isBrowserReady;
    }

    public void loadPage() {
      initJSQueries();

      String htmlContent = Utils.loadHtmlFromResouce("template.html", SwitchEnvironmentAction.currentEnv);

      if (htmlContent == null || htmlContent.isEmpty()) {
        LOG.info("By AI CR Plugin error: 重新安装或联系插件作者。");
        System.out.print("Error: htmlContent is null or empty \n");
        return;
      }
      // load template
      browser.loadHTML(htmlContent);

      // inject javascript code to webview
//      String queryFunctionInjectJS = getQueryFunctionInjectJS();
      String askFunctionsInjectJS = getAskFunctionsInjectJS();
      String diffInjectJS = getDiffInjectJS();
      String askFileInjectJS = getAskFileInjectJS();
      String openFileInjectJS = getOpenFileInjectJS();

      browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
        @Override
        public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
          if (frame.isMain()) { // Check if it's the main frame

            cefBrowser.executeJavaScript("""
              %s
              %s
              %s
              %s
              """.formatted(askFunctionsInjectJS, diffInjectJS, askFileInjectJS, openFileInjectJS), cefBrowser.getURL(), 1);

            if (SwitchEnvironmentAction.currentEnv.equals("development")) {
              browser.openDevtools();
            }

          }
        }

        @Override
        public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
          isBrowserReady = true;
        }
      }, browser.getCefBrowser());
    }

    public String getQueryFunctionInjectJS() {
      JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);

      jsQuery.addHandler((fnName) -> {
        System.out.print("queryFunction handler:  fnName=" + fnName + "\n");

        SwingUtilities.invokeLater(() -> {
          Editor editor = Utils.getCurrentSelectedEditor(projectInstance);
          if (editor == null) {
            LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
            System.out.println("Error: editor is null in Function: getQueryFunctionInjectJS \n");

            return;
          }

          PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, projectInstance);
          if (psiFile == null) {
            LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
            System.out.println("Error: psiFile is null in Function: getQueryFunctionInjectJS \n");
            return;
          }

          // 遍历所有 PsiElement
          psiFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
              super.visitElement(element);

              if (Objects.equals(ElementUtils.getMethodName(element), fnName)) {
                String jsOriginalCode = """
                    sendSingleFunction({
                      fnCode: "{fnCode}",
                      fnName: "{fnName}",
                      filePath: "{filePath}",
                      fileType: "{fileType}",
                    });
                  """;

                String fnCode = Utils.getEscapedCode(element.getText());

                String jsCode = jsOriginalCode.replace("{fnCode}", fnCode).replace("{fnName}", ElementUtils.getMethodName(element)).replace("{filePath}", ElementUtils.getFilePath(element)).replace("{fileType}", Utils.getFileLanguage(element.getContainingFile().getVirtualFile()));

                browser.getCefBrowser().executeJavaScript(jsCode, "", 0);

                System.out.print("update code of " + fnName + "\n");
              }
            }
          });
        });

        return new JBCefJSQuery.Response("IntelliJ Ide");
      });

      return "window.queryFunction = function(fnName) { " + jsQuery.inject("fnName") + "};";
    }

    public String getAskFunctionsInjectJS() {
//      JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);

      askFunctionQuery.addHandler((query) -> {

        recursiveElement2Send();

        return new JBCefJSQuery.Response("Functions Asked");
      });

      return "window.askFunctions = function() { " + askFunctionQuery.inject("") + "};";
    }


    public String getDiffInjectJS() {
//      JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);


      diffQuery.addHandler((result) -> {

        try {
          JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
          String original = jsonObject.get("original").getAsString();
          String optimization = jsonObject.get("optimization").getAsString();

          SwingUtilities.invokeLater(() -> {
            Utils.showDiffView(projectInstance, original, optimization);
          });
          // 处理这两个参数...

        } catch (Exception e) {
          e.printStackTrace();
        }

        return new JBCefJSQuery.Response("Diff Showed");
      });

      return "window.triggerDiff = function(data) {" + diffQuery.inject("data") + "};";
    }

    public String getAskFileInjectJS() {
//      JBCefJSQuery jsQuery = JBCefJSQuery.create((JBCefBrowserBase) browser);

      askFileQuery.addHandler((query) -> {
        addProjectListener(projectInstance);

        Editor editor = Utils.getCurrentSelectedEditor(projectInstance);
        if (editor == null) {
          System.out.print("Error: editor is null in Function: getAskFileInjectJS \n");
          LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
          return new JBCefJSQuery.Response("Ask File Finish");
        }

        VirtualFile file = editor.getVirtualFile();
        if (file == null) {
          System.out.print("Error: file is null in Function: getAskFileInjectJS \n");
          LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
          return new JBCefJSQuery.Response("Ask File Finish");
        }

        sendCurrentFileinfo(editor.getVirtualFile());

        return new JBCefJSQuery.Response("Ask File Succeed");
      });

      return "window.askFileInfo = function() {" + askFileQuery.inject("") + "};";
    }

    public String getOpenFileInjectJS() {
      openFileQuery.addHandler((result) -> {
        System.out.print("result" + result);

        JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
        String filePath = jsonObject.get("filePath").getAsString();
        int start = jsonObject.get("start").getAsInt();

        Utils.openFile(projectInstance, filePath, start);

        return new JBCefJSQuery.Response("File Opened");
      });

      return "window.openFile = function(data) {" + openFileQuery.inject("data") + "};";
    }

    public JBCefBrowser getBrowser() {
      return browser;
    }
  }

  private static void addProjectListener(@NotNull Project project) {
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        System.out.println("selectionChanged: start \n");
        // find all functions in file and send to remote
        recursiveElement2Send();

        VirtualFile file = event.getNewFile();
        if (file == null) {
          LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
          System.out.print("Error: file is null in Function: addProjectListener selectionChanged \n");
          return;
        }

        System.out.println("selectionChanged: file->" + file + "\n");
        sendCurrentFileinfo(file);
      }

      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull com.intellij.openapi.vfs.VirtualFile file) {
        System.out.println("fileOpened: file->" + file + "\n");
        Editor editor = source.getSelectedTextEditor();

        if (editor == null) {
          LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
          System.out.print("Error: current editor is null in Function: addProjectListener \n");
          return;
        }

        System.out.println("fileOpened: editor -> " + editor + "\n");

        editor.getCaretModel().addCaretListener(new CaretListener() {
          @Override
          public void caretPositionChanged(@NotNull CaretEvent event) {
            String selectText = editor.getSelectionModel().getSelectedText();

            if (selectText == null || selectText.isEmpty()) {
              return;
            }

            int selectionStart = editor.getSelectionModel().getSelectionStart();
            int selectionEnd = editor.getSelectionModel().getSelectionEnd();

            int startLine = editor.getDocument().getLineNumber(selectionStart) + 1; // +1 因为行号从0开始
            int endLine = editor.getDocument().getLineNumber(selectionEnd) + 1;

            CefBrowser cefBrowser = toolWindowContent.browser.getCefBrowser();

            String codes = Utils.getEscapedCode(selectText);

            String jsOriginalCode = """
                sendSelectionCode({
                  codes: "{codes}",
                  start: "{startLine}",
                  end: "{endLine}",
                  filePath: "{filePath}",
                  fileType: "{fileType}"
                });
              """;

            String jsCode = jsOriginalCode.replace("{codes}", codes).replace("{startLine}", String.format("%d", startLine)).replace("{endLine}", String.format("%d", endLine)).replace("{filePath}", file.getPath()).replace("{fileType}", Utils.getFileLanguage(file));

//              System.out.println(String.format("Selection - start line: %d, end line: %d",
//                startLine, endLine));
//
//              System.out.println(String.format("Selection - %s", selectText));

            cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 0);

          }
        });
      }
    });
  }

  private static void recursiveElement2Send() {
    // 确保在 EDT 中执行
    SwingUtilities.invokeLater(() -> {
      Editor editor = Utils.getCurrentSelectedEditor(projectInstance);
      if (editor == null) {
        LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
        System.out.print("Error: editor is null in Function: recursiveElement2Send");
        return;
      }

      // 获取当前文件的PSI表示
      PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, projectInstance);
      if (psiFile == null) {
        LOG.info("By AI CR Plugin tip: 打开一个文件获取插件完整能力。");
        System.out.print("Error: psiFile is null in Function: recursiveElement2Send");
        return;
      }

      ArrayList<Map<String, String>> methodList = new ArrayList<>();

      // 遍历所有 PsiElement
      psiFile.accept(new PsiRecursiveElementVisitor() {
        @Override
        public void visitElement(@NotNull PsiElement element) {
          super.visitElement(element);

          if (ElementUtils.isMethodOrFunction(element)) {
            Map<String, String> method = new HashMap<>();
            method.put("fnName", ElementUtils.getMethodName(element));
            // no need to escape here, Gson.toJson add escape
            method.put("codes", element.getText());
            method.put("fileType", Utils.getFileLanguage(element.getContainingFile().getVirtualFile()));
            method.put("filePath", ElementUtils.getFilePath(element));

            // Get the document from the PsiFile
            Document document = PsiDocumentManager.getInstance(projectInstance).getDocument(element.getContainingFile());
            if (document != null) {
              int startOffset = element.getTextRange().getStartOffset();
              int endOffset = element.getTextRange().getEndOffset();
              int startLine = document.getLineNumber(startOffset) + 1; // +1 because line numbers are 0-based
              int endLine = document.getLineNumber(endOffset) + 1;

              method.put("start", String.valueOf(startLine));
              method.put("end", String.valueOf(endLine));
            }

            methodList.add(method);

            Gson gson = new Gson();
            String methodListJson = gson.toJson(methodList);

            String jsOriginalCode = """
                sendFileFunctions(%s);
              """;

            String jsCode = String.format(jsOriginalCode, methodListJson);

            toolWindowContent.getBrowser().getCefBrowser().executeJavaScript(jsCode, "", 0);
          }
        }
      });

    });
  }

  private static void sendCurrentFileinfo(VirtualFile file) {
    String jsOriginalCode = """
        sendFileInfo({
          fileName: "{fileName}",
          filePath: "{filePath}",
          language: "{fileType}"
        });
      """;
    String jsCode = jsOriginalCode.replace("{fileName}", file.getName()).replace("{filePath}", file.getPath()).replace("{fileType}", Utils.getFileLanguage(file));

    toolWindowContent.getBrowser().getCefBrowser().executeJavaScript(jsCode, "", 0);
  }
}
