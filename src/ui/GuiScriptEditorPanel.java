package ui;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Sergey Karashevich
 */
public class GuiScriptEditorPanel {
    private JButton runButton;
    private JPanel editorPanel;
    private JButton recButton;
    private JButton stopButton;
    private JPanel buttonRow;
    private JPanel myPanel;
    private JLabel myStatusLabel;
    private JPanel myStatusBarPanel;
    private AsyncProcessIcon progressIcon;
    private JButton updateButton;

    public GuiScriptEditorPanel() {
        super();
        myStatusLabel.setFont(SystemInfo.isMac ? JBUI.Fonts.label(11) : JBUI.Fonts.label());
        progressIcon.setVisible(false);

        progressIcon.suspend();
        editorPanel.add(GuiScriptEditor.INSTANCE.getPanel(), BorderLayout.CENTER);
        updateButton.setAction(new AbstractAction("Update") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiScriptEditor.INSTANCE.update();
            }
        });
        recButton.setAction(new AbstractAction("Rec") {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus("Start recording GUI script");
            }
        });
        stopButton.setAction(new AbstractAction("Stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus("Recording GUI script stopped");
            }
        });
        runButton.setAction(new AbstractAction("Run") {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus("Perform selected GUI script");
            }
        });
    }

    public void setRunButtonAction(Action action) {
        runButton.setAction(action);
    }

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
