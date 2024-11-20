package org.example.ai_cr_plugin;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ElementUtils {
  static String getMethodName(@NotNull PsiElement element) {
    Language language = element.getLanguage();

    return switch (language.getID()) {
      case "JAVA" -> {
        String className = element.getClass().getName();
        if (className.endsWith("PsiMethodImpl")) {
          // 遍历子节点找到标识符
          for (PsiElement child : element.getChildren()) {
            if (child.getNode().getElementType().toString().equals("IDENTIFIER")) {
              yield child.getText();
            }
          }
        }
        yield "";
      }
      case "Python" -> {
        // 遍历子节点找到标识符
        for (PsiElement child : element.getChildren()) {
          if (child.getNode().getElementType().toString().equals("IDENTIFIER")) {
            yield child.getText();
          }
        }
        yield "";
      }
      case "go" -> {
        try {
          // 尝试调用 getName 方法
          java.lang.reflect.Method getNameMethod = element.getClass().getMethod("getName");
          Object name = getNameMethod.invoke(element);
          if (name != null) {
            yield name.toString();
          }
          
          // 如果上面失败，尝试获取 getIdentifier
          java.lang.reflect.Method getIdentifierMethod = element.getClass().getMethod("getIdentifier");
          Object identifier = getIdentifierMethod.invoke(element);
          if (identifier instanceof PsiElement) {
            yield ((PsiElement) identifier).getText();
          }
        } catch (Exception e) {
          // 如果反射失败，回退到正则方式
          String code = element.getText();
          Pattern pattern = Pattern.compile("func\\s*(?:\\([^)]+\\)\\s*)?(\\w+)\\s*\\(");
          Matcher matcher = pattern.matcher(code);
          if (matcher.find()) {
            yield matcher.group(1);
          }
        }
        
        yield "";
      }
      case "ObjectiveC" -> {
        for (PsiElement child : element.getChildren()) {
          if (child.getNode().getElementType().toString().equals("IDENTIFIER")) {
            yield child.getText();
          }
        }
        yield "";
      }
      case "ECMAScript 6", "TypeScript", "JavaScript", "TypeScript JSX" -> {
        try {
          // 尝试调用 getName 方法
          java.lang.reflect.Method getNameMethod = element.getClass().getMethod("getName");
          Object name = getNameMethod.invoke(element);
          if (name != null) {
            yield name.toString();
          }

          // 如果上面失败，尝试获取 nameIdentifier
          java.lang.reflect.Method getNameIdentifierMethod = element.getClass().getMethod("getNameIdentifier");
          Object nameIdentifier = getNameIdentifierMethod.invoke(element);
          if (nameIdentifier instanceof PsiElement) {
            yield ((PsiElement) nameIdentifier).getText();
          }
        } catch (Exception e) {
          System.out.println("Failed to get function name: " + e.getMessage());
        }
        yield "";
      }
      case "Dart" -> {
        for (PsiElement child : element.getChildren()) {
          if (child.getNode().getElementType().toString().equals("COMPONENT_NAME")) {
            yield child.getText();
          }
        }
        yield "";
      }
      default -> "";
    };
  }

  public static boolean isMethodOrFunction(@NotNull PsiElement element) {
    Language language = element.getLanguage();
    String elementType = element.getNode().getElementType().toString();
    // System.out.print("isMethodOrFunction: " + language.getID() + " Element name" + element.getText() + "\n");
    return switch (language.getID()) {
      case "JAVA" -> {
        String className = element.getClass().getName();

        // 更严格的检查
        boolean isMethod = className.endsWith("PsiMethodImpl") &&  // 只匹配具体的方法实现类
          elementType.equals("METHOD");            // 确保元素类型就是 METHOD

//        // 调试信息
//        if (className.contains("Method")) {
//          System.out.println("Class: " + className + ", Type: " + elementType + ", IsMethod: " + isMethod);
//        }

        yield isMethod;
      }

      case "Python" -> {

        String className = element.getClass().getName();
        boolean isPyFunction = className.contains("PyFunction");
        boolean isFunctionDeclaration = elementType.toLowerCase().contains("function");

        yield isPyFunction || isFunctionDeclaration;
      }
      case "go" -> {
        // Debug info
        //   System.out.println("Go element type: " + elementType);
        //   System.out.println("Go class name: " + element.getClass().getName());

        yield elementType.equals("FUNCTION_DECLARATION") ||    // 普通函数
          elementType.equals("METHOD_DECLARATION") ||      // 方法
          elementType.equals("INTERFACE_METHOD") ||        // 接口方法
          elementType.contains("FUNC") ||                  // 其他可能的函数形式
          element.getClass().getName().contains("GoFunctionOrMethodDeclaration");
      }
      case "ObjectiveC" -> {

        boolean isFunction = elementType.contains("FUNCTION") ||
          elementType.contains("METHOD") ||
          elementType.equals("COMPOUND_STATEMENT") ||
          element.getClass().getName().contains("OCMethod") ||     // 添加 ObjectiveC 方法支持
          element.getClass().getName().contains("OCFunction") ||   // 保留 ObjectiveC 函数支持
          element.getClass().getName().contains("CppFunction");    // 保留 C++ 支持

//        System.out.println("Is C++/ObjC function: " + isFunction + element.getClass().getName());
        yield isFunction;
      }
      case "ECMAScript 6", "TypeScript", "JavaScript", "TypeScript JSX" -> {
        String className = element.getClass().getName();

        boolean isFunction = elementType.equals("FUNCTION_DECLARATION") ||    // 普通函数声明
                           elementType.equals("METHOD") ||                    // 类方法
                           elementType.equals("ARROW_FUNCTION") ||           // 箭头函数
                           elementType.contains("FUNCTION_EXPRESSION") ||    // 函数表达式
                            elementType.contains("TYPESCRIPT_FUNCTION") ||
                           className.contains("JSFunction");                 // JS函数实现类

        // Debug info
        // System.out.println("JS element type: " + elementType + "\n");
        // System.out.println("JS class name: " + className);
        // System.out.println("Is JS function: " + isFunction);

        yield isFunction;
      }
      case "Dart" -> {
        String className = element.getClass().getName();

        boolean isFunction = elementType.equals("METHOD_DECLARATION") ||    // 类方法
                           elementType.equals("FUNCTION_DECLARATION") ||    // 顶层函数
                           className.contains("DartMethodDeclaration") ||   // Dart方法声明
                           className.contains("DartFunctionDeclarationWithBody"); // Dart函数声明

        // Debug info
        // System.out.println("Dart element type: " + elementType);
        // System.out.println("Dart class name: " + className);
        // System.out.println("Is Dart function: " + isFunction);

        yield isFunction;
      }
      default -> false;
    };
  }

  public static String getFilePath(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file.getVirtualFile().getPath();
  }
}
