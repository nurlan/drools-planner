/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.planner.examples.cloudbalancing.swingui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.drools.planner.examples.cloudbalancing.domain.CloudAssignment;
import org.drools.planner.examples.cloudbalancing.domain.CloudBalance;
import org.drools.planner.examples.cloudbalancing.domain.CloudComputer;
import org.drools.planner.examples.cloudbalancing.domain.CloudProcess;
import org.drools.planner.examples.cloudbalancing.solver.move.CloudComputerChangeMove;
import org.drools.planner.examples.common.swingui.SolutionPanel;
import org.drools.planner.examples.nurserostering.domain.Assignment;
import org.drools.planner.examples.nurserostering.domain.Employee;
import org.drools.planner.examples.nurserostering.domain.ShiftDate;
import org.drools.planner.examples.nurserostering.solver.move.EmployeeChangeMove;

/**
 * TODO this code is highly unoptimized
 * @author Geoffrey De Smet
 */
public class CloudBalancingPanel extends SolutionPanel {

    private static final Color HEADER_COLOR = Color.YELLOW;


    public CloudBalancingPanel() {
        setLayout(new GridLayout(0, 1));
    }

    private CloudBalance getCloudBalance() {
        return (CloudBalance) solutionBusiness.getSolution();
    }

    public void resetPanel() {
        removeAll();
        CloudBalance cloudBalance = getCloudBalance();
        add(createHeaderPanel());
        List<CloudComputer> cloudComputerList = cloudBalance.getCloudComputerList();
        Map<CloudComputer, CloudComputerPanel> computerToPanelMap
                = new HashMap<CloudComputer, CloudComputerPanel>(cloudComputerList.size());
        for (CloudComputer cloudComputer : cloudComputerList) {
            CloudComputerPanel cloudComputerPanel = new CloudComputerPanel(cloudComputer);
            computerToPanelMap.put(cloudComputer, cloudComputerPanel);
            add(cloudComputerPanel);
        }
        if (cloudBalance.isInitialized()) {
            for (CloudAssignment cloudAssignment : cloudBalance.getCloudAssignmentList()) {
                CloudComputer cloudComputer = cloudAssignment.getCloudComputer();
                CloudComputerPanel cloudComputerPanel = computerToPanelMap.get(cloudComputer);
                cloudComputerPanel.addCloudAssignment(cloudAssignment);
            }
        }
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridLayout(1, 0));
        JLabel cloudComputerLabel = new JLabel("CloudComputer");
        cloudComputerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        cloudComputerLabel.setBackground(HEADER_COLOR);
        cloudComputerLabel.setOpaque(true);
        headerPanel.add(cloudComputerLabel);
        JLabel cloudProcessLabel = new JLabel("CloudProcess");
        cloudProcessLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        cloudProcessLabel.setOpaque(true);
        headerPanel.add(cloudProcessLabel);
        return headerPanel;
    }

    private class CloudComputerPanel extends JPanel {

        private final CloudComputer cloudComputer;

        public CloudComputerPanel(CloudComputer cloudComputer) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.cloudComputer = cloudComputer;
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            JTextArea cloudComputerLabel = new JTextArea(cloudComputer.getLabel(), 4, 14);
            cloudComputerLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            cloudComputerLabel.setBackground(HEADER_COLOR);
            cloudComputerLabel.setEditable(false);
            add(cloudComputerLabel);
        }

        public void addCloudAssignment(CloudAssignment cloudAssignment) {
            JPanel cloudAssignmentPanel = new JPanel();
            cloudAssignmentPanel.setLayout(new BoxLayout(cloudAssignmentPanel, BoxLayout.X_AXIS));
            JButton button = new JButton(new CloudAssignmentAction(cloudAssignment));
            cloudAssignmentPanel.add(button);
            cloudAssignmentPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            JTextArea cloudAssignmentLabel = new JTextArea(cloudAssignment.getLabel(), 4, 14);
            cloudAssignmentLabel.setEditable(false);
            cloudAssignmentPanel.add(cloudAssignmentLabel);
            add(cloudAssignmentPanel);
        }

    }

    private class CloudAssignmentAction extends AbstractAction {

        private CloudAssignment cloudAssignment;

        public CloudAssignmentAction(CloudAssignment cloudAssignment) {
            super("Id " + cloudAssignment.getId());
            this.cloudAssignment = cloudAssignment;
        }

        public void actionPerformed(ActionEvent e) {
            List<CloudComputer> cloudComputerList = getCloudBalance().getCloudComputerList();
            JComboBox cloudComputerListField = new JComboBox(cloudComputerList.toArray());
            cloudComputerListField.setSelectedItem(cloudAssignment.getCloudComputer());
            int result = JOptionPane.showConfirmDialog(CloudBalancingPanel.this.getRootPane(), cloudComputerListField,
                    "Select cloud computer", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                CloudComputer toCloudComputer = (CloudComputer) cloudComputerListField.getSelectedItem();
                solutionBusiness.doMove(new CloudComputerChangeMove(cloudAssignment, toCloudComputer));
                workflowFrame.updateScreen();
            }
        }

    }

}