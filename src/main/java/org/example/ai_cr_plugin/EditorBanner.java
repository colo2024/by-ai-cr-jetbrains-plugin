//package org.example.ai_cr_plugin;
//
//import com.intellij.openapi.fileEditor.FileEditor;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowManager;
//import com.intellij.ui.EditorNotificationPanel;
//import com.intellij.ui.EditorNotificationProvider;
//import com.intellij.ui.jcef.JBCefBrowser;
//import org.cef.browser.CefBrowser;
//import org.cef.browser.CefFrame;
//import org.cef.handler.CefLoadHandlerAdapter;
//
//import javax.swing.*;
//import java.io.IOException;
//import java.util.function.Function;
//
//public class EditorBanner implements EditorNotificationProvider {
//
//  @Override
//  public Function<FileEditor, JComponent> collectNotificationData(Project project, VirtualFile virtualFile) {
//    return new Function<FileEditor, JComponent>() {
//      @Override
//      public JComponent apply(FileEditor fileEditor) {
//        EditorNotificationPanel banner = new EditorNotificationPanel();
//        banner.setText("AI help to review your code.");
//        banner.setToolTipText("Open CR Tool Window");
//
//        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
//        ToolWindow toolWindow = toolWindowManager.getToolWindow("ToolwindowPanel");
//
//        banner.createActionLabel("By AI Code Review", () -> {
//          if (toolWindow == null) {
//            Messages.showMessageDialog("Tool window is null, restart the IDE and try again", "Error", Messages.getErrorIcon());
//            return;
//          }
//
//          // ensure the tool window is hidden, to avoid open multiple tool window
//          toolWindow.hide();
//
//          if (virtualFile == null) {
//            Messages.showMessageDialog("You haven't select a file", "Error", Messages.getErrorIcon());
//            return;
//          }
//
//          String fileContent = "";
//          try {
//            fileContent = new String(virtualFile.contentsToByteArray());
//          } catch (IOException ex) {
//            Messages.showMessageDialog(ex.getMessage(), "Error", Messages.getErrorIcon());
//            ex.printStackTrace();
//            return;
//          }
//
//          String fileTypeName = Utils.getFileLanguage(virtualFile);
//
////        System.out.print("------- fileContent editor banner ------" + fileContent);
//          String htmlContent = Utils.loadHtmlFromResouce("template.html", fileContent, fileTypeName);
////          System.out.print("------- editor banner htmlContent ------" + htmlContent);
//
//          toolWindow.show(() -> {
//            // 使用 toolWindowContent 来更新内容
//            ToolWindowPanelFactory.CRToolWindowContent toolWindowContent = ToolWindowPanelFactory.getToolWindowContent();
//            if (toolWindowContent == null) {
//              Messages.showMessageDialog("Plugin hasn't been initiated, wait second and please try again or restart the IDE.", "Error", Messages.getErrorIcon());
//              return;
//            }
//
//            toolWindowContent.updateContent(htmlContent);
//
//            JBCefBrowser browser = toolWindowContent.getBrowser();
//
//            String jsCode = toolWindowContent.getDiffInjectJS();
//
//            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
//              @Override
//              public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
//                if (frame.isMain()) { // Check if it's the main frame
//
//                  cefBrowser.executeJavaScript(jsCode, cefBrowser.getURL(), 1);
//
//                  // browser.openDevtools();
//                }
//              }
//            }, browser.getCefBrowser());
//          });
//
//        });
//
//        banner.createActionLabel("Dismiss", () -> {
//          banner.setVisible(false); // 关闭面板
//        });
//        return banner;
//      }
//    };
//  }
//}
