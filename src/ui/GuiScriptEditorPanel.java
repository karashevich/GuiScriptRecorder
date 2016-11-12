package ui;

import actions.PerformScriptAction;
import actions.StartStopRecAction;
import actions.UpdateEditorAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author Sergey Karashevich
 */
public class GuiScriptEditorPanel {
    private JButton runButton;
    private JPanel editorPanel;
    private JPanel myPanel;
    private JLabel myStatusLabel;
    private JPanel myStatusBarPanel;
    private AsyncProcessIcon progressIcon;
    private JPanel iconButtonRow;

    private GuiScriptEditor myEditor;

    public GuiScriptEditorPanel() {
        super();
        myStatusLabel.setFont(SystemInfo.isMac ? JBUI.Fonts.label(11) : JBUI.Fonts.label());
        progressIcon.setVisible(false);

        myEditor = new GuiScriptEditor();

        progressIcon.suspend();
        editorPanel.add(myEditor.getPanel(), BorderLayout.CENTER);

        installActionToolbar();
    }

    public Editor getEditor(){
        return myEditor.getMyEditor();
    }

    public void releaseEditor(){
        myEditor.releaseEditor();
    }

    private void installActionToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new StartStopRecAction());
        group.add(new UpdateEditorAction());
        group.add(new PerformScriptAction());
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);

        iconButtonRow.add(toolbar.getComponent(), BorderLayout.CENTER);
    }

    public void setRunButtonAction(Action action) {
        runButton.setAction(action);
    }

    public void setUpdateButtonAction(Action action) { runButton.setAction(action); }

    public Component getPanel(){
        return myPanel;
    }

    public void updateStatus(String status){
        myStatusLabel.setText(status);
        myStatusLabel.repaint();
    }


    public void updateStatusWithProgress(String statusWithProgress){
        progressIcon.setVisible(true);
        progressIcon.resume();
        myStatusLabel.setText(statusWithProgress);
        myStatusLabel.repaint();
    }

    public void stopProgress(){
        progressIcon.setVisible(false);
        progressIcon.suspend();
    }

    private void createUIComponents() {
        progressIcon = new AsyncProcessIcon("Progress");
    }

    private class MyTextPanel extends TextPanel{
        MyTextPanel(){
            super();
        }
    }

}