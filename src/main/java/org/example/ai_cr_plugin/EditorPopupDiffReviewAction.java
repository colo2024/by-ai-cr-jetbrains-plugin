//package org.example.ai_cr_plugin;
//
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.openapi.vcs.VcsException;
//import com.intellij.openapi.vcs.history.VcsFileRevision;
//import com.intellij.openapi.vcs.history.VcsHistoryProvider;
//import com.intellij.openapi.vcs.history.VcsHistorySession;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowManager;
//import com.intellij.ui.jcef.JBCefBrowser;
//import com.intellij.vcsUtil.VcsUtil;
//import org.cef.browser.CefBrowser;
//import org.cef.browser.CefFrame;
//import org.cef.handler.CefLoadHandlerAdapter;
//import org.jetbrains.annotations.NotNull;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class EditorPopupDiffReviewAction extends AnAction {
//
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//        Project project = e.getProject();
//        if (project == null) return;
//
//        // 获取当前文件
//        VirtualFile currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
//        if (currentFile == null) return;
//
//        String currentContent = getCurrentContent(currentFile);
//
//        String lastCommitContent = null;
//        try {
//            lastCommitContent = getLastCommitContent(project, currentFile);
//        } catch (VcsException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        Utils.showDiffView(project, currentContent, lastCommitContent);
//
//        String diffContent = getChangedCode(project, currentFile);
//        String fileTypeName = Utils.getFileLanguage(currentFile);
//
//        // 打开 ToolWindow 并显示内容
//        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("ToolwindowPanel");
//
//        if (toolWindow == null) {
//            Messages.showMessageDialog("Tool window is null, restart the IDE and try again", "Error", Messages.getErrorIcon());
//            return;
//        }
//
//        toolWindow.show(() -> {
//            // 使用 toolWindowContent 来更新内容
//            ToolWindowPanelFactory.CRToolWindowContent toolWindowContent = ToolWindowPanelFactory.getToolWindowContent();
//            if (toolWindowContent == null) {
//                Messages.showMessageDialog("Plugin hasn't been initiated, wait second and please try again or restart the IDE.", "Error", Messages.getErrorIcon());
//                return;
//            }
//
//            String htmlContent = Utils.loadHtmlFromResouce("template.html", diffContent, fileTypeName);
//            System.out.print("------- editor popup htmlContent ------" + htmlContent);
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
//
//    }
//
//    private String getCurrentContent(VirtualFile file) {
//        try {
//            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            return "";
//        }
//    }
//
//    public String getLastCommitContent(Project project, VirtualFile virtualFile) throws VcsException {
//        // 获取文件的历史
//        VcsHistoryProvider historyProvider = Objects.requireNonNull(VcsUtil.getVcsFor(project, virtualFile)).getVcsHistoryProvider();
//        VcsHistorySession session = null;
//        if (historyProvider != null) {
//            session = historyProvider.createSessionFor(VcsUtil.getFilePath(virtualFile));
//        }
//
//        // 获取最近的修订
//        List<VcsFileRevision> revisions = null;
//        if (session != null) {
//            revisions = session.getRevisionList();
//        }
//        if (revisions != null && revisions.isEmpty()) {
//            return null; // 没有历史记录
//        }
//
//
//        // 获取最近的修订
//        VcsFileRevision lastRevision = null;
//        if (revisions != null) {
//            lastRevision = revisions.get(0);
//        }
//
//        // 获取文件内容
//
//        byte[] content = null;
//        try {
//            if (lastRevision != null) {
//                content = lastRevision.getContent();
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        if (content == null) return null;
//
//        return new String(content, StandardCharsets.UTF_8);
//    }
//
////  private String getLastCommitContent(Project project, VirtualFile file) {
////    try {
////      List<String> command = new ArrayList<>();
////      System.out.print("file relative path" + getRelativePath(project, file));
////
////      command.add("git");
////      command.add("show");
////      command.add("HEAD~1:" + getRelativePath(project, file));
////
////      ProcessBuilder processBuilder = new ProcessBuilder(command);
////      processBuilder.directory(new File(project.getBasePath()));
////      Process process = processBuilder.start();
////
////      StringBuilder output = new StringBuilder();
////      try (BufferedReader reader = new BufferedReader(
////        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
////        String line;
////        while ((line = reader.readLine()) != null) {
////          output.append(line).append("\n");
////        }
////      }
////
////      process.waitFor();
////      return output.toString();
////    } catch (Exception e) {
////      return "";
////    }
////  }
//
//
//    private String getChangedCode(Project project, VirtualFile file) {
//        try {
//            List<String> command = new ArrayList<>();
//            command.add("git");
//            command.add("diff");
//            command.add("HEAD");
//            command.add("--unified=0"); // 只显示变更的行
//            command.add("--");
//            command.add(file.getPath());
//
//            ProcessBuilder processBuilder = new ProcessBuilder(command);
//            processBuilder.directory(new File(project.getBasePath()));
//            Process process = processBuilder.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // 保留 + 和 - 符号，但排除文件头部的 +++ 和 --- 行
//                    if ((line.startsWith("+") || line.startsWith("-"))
//                            && !line.startsWith("+++")
//                            && !line.startsWith("---")) {
//                        output.append(line).append("\n");
//                    }
//                }
//            }
//
//            process.waitFor();
//            return output.toString();
//        } catch (Exception e) {
//            return "";
//        }
//    }
//
//
//    @Override
//    public void update(@NotNull AnActionEvent e) {
//        Project project = e.getProject();
//        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
//
//        boolean hasChanges = false;
//
//        if (project != null && file != null) {
//            // 检查是否在 VCS 控制下
//            if (VcsUtil.getVcsFor(project, file) != null) {
//                try {
//                    // 获取变更内容
//                    String diffContent = getChangedCode(project, file);
//
//                    // 如果有变更内容，则启用菜单项
//                    hasChanges = !diffContent.trim().isEmpty();
//                } catch (Exception ex) {
//                    // 如果出错，禁用菜单项
//                    ex.printStackTrace();
//                }
//            }
//        }
//
//        // 设置菜单项的可见性和可用性
//        e.getPresentation().setEnabled(hasChanges);
//    }
//}
