package org.example.ai_cr_plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.openapi.diagnostic.Logger;


public class Utils {

  private static final Logger LOG = Logger.getInstance(ToolWindowPanelFactory.class);

  public static String loadHtmlFromResouce(String resourceName, String env) {
    var inputStream = Utils.class.getClassLoader().getResourceAsStream(resourceName);
    var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

    String htmlContentString = reader.lines().collect(Collectors.joining(System.lineSeparator()));

    return env == null ? htmlContentString : htmlContentString.replace("{env}", env);
  }

  public static String getHtmlContentFromResource(String resourceName) {
    var inputStream = Utils.class.getClassLoader().getResourceAsStream(resourceName);
    BufferedReader reader = null;
    if (inputStream != null) {
      reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    if (reader != null) {
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } else {
      return "";
    }
  }

  public static String getFileLanguage(VirtualFile virtualFile) {
    FileType fileType = virtualFile.getFileType();
    String fileTypeName; // Returns "JAVA", "JavaScript", etc.
    String extension = virtualFile.getExtension();

    if (extension != null) {
      switch (extension.toLowerCase()) {
        case "ts":
        case "tsx":
          fileTypeName = "TypeScript";
          break;
        case "js":
        case "jsx":
          fileTypeName = "JavaScript";
          break;
        case "css":
        case "scss":
        case "less":
          fileTypeName = "CSS";
          break;
        default:
          fileTypeName = fileType.getName();
      }
    } else {
      fileTypeName = fileType.getName();
    }
    return fileTypeName;
  }

  public static Editor getCurrentSelectedEditor(Project project) {
    // 获取 FileEditorManager 实例
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

    // 获取当前激活的编辑器
    FileEditor currentEditor = fileEditorManager.getSelectedEditor();

    // 检查当前编辑器是否为 TextEditor 并返回其 Editor
    if (currentEditor instanceof TextEditor) {
      return ((TextEditor) currentEditor).getEditor();
    }

    // 如果不是 TextEditor，或者没有打开的编辑器，则返回 null
    return null;
  }

  public static void showDiffView(Project project, String currentString, String providedString) {
    DiffContentFactory contentFactory = DiffContentFactory.getInstance();
    DiffContent currentContent = contentFactory.create(currentString);
    DiffContent providedContent = contentFactory.create(providedString);

    DiffManager.getInstance().showDiff(

      project,

      new SimpleDiffRequest(
        "Diff of Current and Provided Code",
        currentContent,
        providedContent,
        "Current Code",
        "Provided Code"
      )
    );
  }

  public static String getEscapedCode(String code) {
    return code.replace("\\", "\\\\") // 转义反斜杠
      .replace("\"", "\\\"") // 转义双引号
      .replace("\n", "\\n")  // 转义换行符
      .replace("\r", "\\r");
  }


  public static void openFile(Project project, String filePath, int lineNumber) {
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null && file.exists()) {
          // 打开文件并获取编辑器
          FileEditor[] editors = FileEditorManager.getInstance(project).openFile(file, true);

          // 只有当lineNumber有效时才进行光标移动
          if (lineNumber > 0 && editors.length > 0 && editors[0] instanceof TextEditor) {
            Editor editor = ((TextEditor) editors[0]).getEditor();
            Document document = editor.getDocument();

            // 确保行号在有效范围内
            int targetLine = Math.min(Math.max(0, lineNumber - 1), document.getLineCount() - 1);
            int offset = document.getLineStartOffset(targetLine);

            // 移动光标并使该行可见
            editor.getCaretModel().moveToOffset(offset);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to open file: " + filePath, e);
      }
    });
  }
}
