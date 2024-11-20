package org.example.ai_cr_plugin;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.InlayHintsPass;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Timer;

public class FileOpenListener implements FileEditorManagerListener {
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Project project = source.getProject();
        Editor editor = source.getSelectedTextEditor();

        if (editor == null) return;

        // 首次尝试
        scheduleHintsUpdate(project, editor, 100);  // 100ms
        // 如果首次失败，500ms 后重试
        scheduleHintsUpdate(project, editor, 500);  // 500ms
        // 最后一次尝试
        scheduleHintsUpdate(project, editor, 1500); // 1500ms
    }

    private void scheduleHintsUpdate(Project project, Editor editor, int delay) {
        new Timer(delay, e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) return;

                // 获取当前文件的 PsiFile
                PsiFile psiFile = PsiDocumentManager.getInstance(project)
                    .getPsiFile(editor.getDocument());

                if (psiFile != null) {
                    // 强制重新分析当前文件
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
                }
            });
        }).start();
    }
}
