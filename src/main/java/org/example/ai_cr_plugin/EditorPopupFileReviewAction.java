//package org.example.ai_cr_plugin;
//
//import com.intellij.openapi.actionSystem.*;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowManager;
//import com.intellij.ui.jcef.JBCefBrowser;
//import org.cef.browser.CefBrowser;
//import org.cef.browser.CefFrame;
//import org.cef.handler.CefLoadHandlerAdapter;
//
//public class EditorPopupFileReviewAction extends AnAction {
//
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//        // TODO: insert action logic here
//        Project project = e.getProject();
//
//        if (project == null) {
//            Messages.showMessageDialog("Project is null, open a project and try again", "Error", Messages.getErrorIcon());
//            return;
//        }
//
//        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
//        ToolWindow toolWindow = toolWindowManager.getToolWindow("ToolwindowPanel");
//
//        if (toolWindow == null) {
//            Messages.showMessageDialog("Tool window is null, restart the IDE and try again", "Error", Messages.getErrorIcon());
//            return;
//        }
//
//        // ensure the tool window is hidden, to avoid open multiple tool window
//        toolWindow.hide();
//
//        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
//
//        if (virtualFile == null) {
//            Messages.showMessageDialog("You haven't select a file", "Attention", Messages.getInformationIcon());
//            return;
//        }
//
//        String fileTypeName = Utils.getFileLanguage(virtualFile);
//
//        String selectContext;
//        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
//        String selectText = editor.getSelectionModel().getSelectedText();
//
//        if (selectText != null && !selectText.trim().isEmpty()) {
//            selectContext = selectText;
//        } else {
//            selectContext = editor.getDocument().getText();
//        }
//
//        String htmlContent = Utils.loadHtmlFromResouce("template.html", selectContext, fileTypeName);
//        System.out.print("------- editor popup htmlContent ------" + htmlContent);
//
//
//        toolWindow.show(() -> {
//            // 使用 toolWindowContent 来更新内容
//            ToolWindowPanelFactory.CRToolWindowContent toolWindowContent = ToolWindowPanelFactory.getToolWindowContent();
//            if (toolWindowContent == null) {
//                Messages.showMessageDialog("Plugin hasn't been initiated, wait second and please try again or restart the IDE.", "Error", Messages.getErrorIcon());
//                return;
//            }
//
//            toolWindowContent.updateContent(htmlContent);
//
//            JBCefBrowser browser = toolWindowContent.getBrowser();
//
//            String jsCode = toolWindowContent.getDiffInjectJS();
//
//            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
//                @Override
//                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
//                    if (frame.isMain()) { // Check if it's the main frame
//                        cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 1);
////            browser.openDevtools();
//                    }
//                }
//            }, browser.getCefBrowser());
//        });
//    }
//
//}
