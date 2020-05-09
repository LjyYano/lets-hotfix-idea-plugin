package com.github.hotreload.component;

import static com.github.hotreload.utils.Constants.DEFAULT_HOST;
import static com.github.hotreload.utils.Constants.NEED_SELECT_JVM_PROCESS;
import static com.github.hotreload.utils.ReloadUtil.filterProcess;
import static com.github.hotreload.utils.ReloadUtil.getHostList;
import static com.github.hotreload.utils.ReloadUtil.getProcessList;
import static com.github.hotreload.utils.ReloadUtil.splitKeywordsText;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;

import com.github.hotreload.config.ApplicationConfig;
import com.github.hotreload.http.HttpServiceFactory;
import com.github.hotreload.model.JvmProcess;
import com.github.hotreload.utils.ReloadUtil;
import com.intellij.openapi.ui.Messages;

/**
 * @author liuzhengyang
 */
public class SettingPanel {

    private JPanel rootPanel;

    private JTextField serverField0;
    private JComboBox<String> hostNameBox0;
    private JTextField keywordFiled0;
    private JComboBox<JvmProcess> processBox0;
    private List<JvmProcess> currentProcessList0;
    private JvmProcess selectedProcess0;
    private JRadioButton selectRadioButton0;

    private JTextField serverField1;
    private JTextField keywordFiled1;
    private JComboBox<String> hostNameBox1;
    private JComboBox<JvmProcess> processBox1;
    private List<JvmProcess> currentProcessList1;
    private JvmProcess selectedProcess1;
    private JRadioButton selectRadioButton1;

    private final int groupNum = 2;
    private int selectNum = -1;

    public JTextField getServerField0() {
        return serverField0;
    }

    public JTextField getKeywordFiled0() {
        return keywordFiled0;
    }

    public JComboBox<JvmProcess> getProcessBox0() {
        return processBox0;
    }

    public JComboBox<String> getHostNameBox0() {
        return hostNameBox0;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public void setData(ApplicationConfig applicationConfig) {
        // todo 换成list
        String serverUrl = applicationConfig.getServer();
        for (int i = 0; i < groupNum; i++) {
            ((JTextField) getField("serverField", i)).setText(serverUrl);
            ((JTextField) getField("keywordFiled", i)).setText(applicationConfig.getKeywordText());
            if (StringUtils.isBlank(serverUrl)) {
                continue;
            }

            // todo
            HttpServiceFactory.setServer(serverUrl);
            setField("selectedProcess", i, applicationConfig.getSelectedProcess());
            fillHostBox(serverUrl, applicationConfig.getSelectedHostName(), i);
        }
    }

    public void getData(ApplicationConfig applicationConfig) {
        for (int i = 0; i < groupNum; i++) {
            applicationConfig.setServer(((JTextField)getField("serverField", i)).getText());
            JComboBox<JvmProcess> processBox = getField("processBox", i);
            if (processBox.getSelectedItem() != null) {
                applicationConfig.setSelectedProcess((JvmProcess) processBox.getSelectedItem());
            }
            applicationConfig.setKeywords(splitKeywordsText(((JTextField)getField("keywordFiled",
                    i)).getText()));
            JComboBox<String> hostNameBox = getField("hostNameBox", i);
            if (hostNameBox.getSelectedItem() != null) {
                applicationConfig.setSelectedHostName(hostNameBox.getSelectedItem().toString());
            }
        }
    }

    public boolean isModifiable(ApplicationConfig applicationConfig) {
        try {
            checkArgument(Objects.equals(serverField0.getText(), applicationConfig.getServer()));
            checkArgument(Objects.equals(hostNameBox0.getSelectedItem(), applicationConfig.getSelectedHostName()));
            checkArgument(Objects.equals(keywordFiled0.getText(), applicationConfig.getKeywordText()));
            checkArgument(Objects.equals(processBox0.getSelectedItem(), applicationConfig.getSelectedProcess()));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void initComponent() {
        addServerFocusListener();
        addHostNameChangeListener();
        addKeywordChangeListener();
        addHorizontalScrollForProcessBox();
        addRatioChangeListener();
    }

    private void addHorizontalScrollForProcessBox() {
        for (int i = 0; i < groupNum; i++) {
            JComboBox<JvmProcess> processBox = getField("processBox", i);
            Object comp = processBox.getUI().getAccessibleChild(processBox, 0);
            if (comp instanceof JPopupMenu) {
                JPopupMenu popup = (JPopupMenu) comp;
                JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
                scrollPane.setAutoscrolls(true);
                scrollPane.setHorizontalScrollBar(new JScrollBar(JScrollBar.HORIZONTAL));
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            }
        }
    }

    private void addServerFocusListener() {
        for (int i = 0; i < groupNum; i++) {
            JTextField serverField = getField("serverField", i);
            int select = i;
            serverField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    // do nothing
                }

                @Override
                public void focusLost(FocusEvent e) {
                    String serverUrl = serverField.getText();
                    if (StringUtils.isEmpty(serverUrl)) {
                        return;
                    }
                    fillHostBox(serverUrl, "", select);
                }
            });
        }
    }

    private void addHostNameChangeListener() {
        for (int i = 0; i < groupNum; i++) {
            JComboBox<String> hostNameBox = getField("hostNameBox", i);
            hostNameBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    return;
                }
                String hostName = e.getItem().toString();
                fillProcessBox(hostName);
            });
        }
    }

    private void addRatioChangeListener() {
        for (int i = 0; i < groupNum; i++) {
            int select = i;
            Optional.ofNullable((JRadioButton) getField("selectRadioButton", select)).
                    ifPresent(jRadioButton -> jRadioButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    selectNum = select;
                    Messages.showErrorDialog("select", selectNum + "");
                    reflectRadioButton();
                }
            }));
        }
    }

    private <T> T getField(String fieldNamePrefix, int num) {
        try {
            Field field = this.getClass().getDeclaredField(fieldNamePrefix + num);
            field.setAccessible(true);
            return  (T) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("Hotfix exception");
        }

        return null;
    }

    private void setField(String fieldNamePrefix, int num, Object object) {
        try {
            Field field = this.getClass().getDeclaredField(fieldNamePrefix + num);
            field.setAccessible(true);
            field.set(this, object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("Hotfix exception");
        }
    }

    private void reflectRadioButton() {
        try {
            Field field = this.getClass().getDeclaredField("selectRadioButton" + selectNum);
            field.setAccessible(true);
            JRadioButton button = (JRadioButton)field.get(this);
            button.setText(button.getText() + "a");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("Hotfix exception");
        }
    }

    private void addKeywordChangeListener() {
        for (int i = 0; i < groupNum; i++) {
            JTextField keywordFiled = getField("keywordFiled", i);
            keywordFiled.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changeShownProcessList();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changeShownProcessList();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    changeShownProcessList();
                }
            });
        }
    }

    private void fillHostBox(String serverUrl, String selectedHostName, int num) {
        if (StringUtils.isBlank(serverUrl)) {
            return;
        }
        HttpServiceFactory.setServer(serverUrl);
        List<String> hosts = getHostList();
        if (isEmpty(hosts)) {
            hosts = singletonList(DEFAULT_HOST);
        }
        JComboBox<String> hostNameBox = getField("hostNameBox", num);
        hostNameBox.removeAllItems();
        hosts.forEach(hostNameBox::addItem);
        if (StringUtils.isBlank(selectedHostName)) {
            return;
        }
        hostNameBox.setSelectedItem(selectedHostName);
    }

    private void fillProcessBox(String hostName) {
        List<JvmProcess> processes = getProcessList(hostName);
        if (isEmpty(processes)) {
            return;
        }
        currentProcessList0 = processes;
        String keywordText = keywordFiled0.getText();
        boolean hasKeywords = false;
        if (isNotBlank(keywordText)) {
            List<String> keywords = splitKeywordsText(keywordText);
            processes = filterProcess(processes, keywords);
            hasKeywords = true;
        }
        processBox0.removeAllItems();
        if (isEmpty(processes)) {
            return;
        }
        if (hasKeywords && processes.size() > 1) {
            processes.add(0, NEED_SELECT_JVM_PROCESS);
        }
        processes.forEach(processBox0::addItem);
        if (selectedProcess0 != null) {
            processBox0.setSelectedItem(selectedProcess0);
        }
        JTextField textField = (JTextField) processBox0.getEditor().getEditorComponent();
        textField.setCaretPosition(0);
    }

    private void changeShownProcessList() {
        if (isEmpty(currentProcessList0)) {
            return;
        }
        String keywordText = keywordFiled0.getText();
        List<JvmProcess> shownProcessList;
        if (StringUtils.isBlank(keywordText)) {
            shownProcessList = currentProcessList0;
        } else {
            List<String> keywords = ReloadUtil.splitKeywordsText(keywordText);
            shownProcessList = ReloadUtil.filterProcess(currentProcessList0, keywords);
        }

        if (shownProcessList.size() > 0) {
            processBox0.removeAllItems();
            shownProcessList.forEach(processBox0::addItem);
            processBox0.showPopup();
        } else {
            processBox0.removeAllItems();
            processBox0.hidePopup();
        }
    }

}
