package org.example.ai_cr_plugin.settings;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

public class SwitchEnvironmentAction extends ToggleAction {
  public static String currentEnv = "production";
//  public static String currentEnv = "development";

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    String actionId = e.getActionManager().getId(this);
    return ("SwitchToDevelopmentAction".equals(actionId) && "development".equals(currentEnv)) ||
           ("SwitchToProductionAction".equals(actionId) && "production".equals(currentEnv));
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    if (state) {
      String actionId = e.getActionManager().getId(this);
      if ("SwitchToDevelopmentAction".equals(actionId)) {
        currentEnv = "development";
      } else if ("SwitchToProductionAction".equals(actionId)) {
        currentEnv = "production";
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
  }
}
