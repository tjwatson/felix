/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.console.gui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.util.Enumeration;
import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.apache.felix.mosgi.console.ifc.Plugin;
import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.remote.JMXConnectionNotification;

public class NodesTree extends JPanel implements TreeSelectionListener, NotificationListener, ActionListener, PropertyChangeListener {

  private static Hashtable PROTOCOL_PACKAGE_PROVIDER = new Hashtable();
  protected static final String TOP_NAME = "Servers";
  private static int POOLING_TIME = 2500;
  
  private Activator activator;
  private static BundleContext bc;
  
  protected JTree tree;
  private DefaultTreeModel dtm;
  private DefaultMutableTreeNode top = null;
  private JButton jb_addNode;
  private JButton jb_removeNode;
  private JTextField jtf_pool;
  private JButton jb_refresh;

  private boolean cont = true; 
  private PoolingThread pt;
  private boolean isAllNodesConnected = false;

  public NodesTree(Activator activator, BundleContext bc) {
    super(new BorderLayout());
    this.activator = activator;
    this.bc = bc;
    this.pt = new PoolingThread();
    
    System.setProperty("java.security.policy", "client.policy");
    top = new DefaultMutableTreeNode(NodesTree.TOP_NAME);
    dtm = new DefaultTreeModel(top);
    tree = new JTree(dtm);
    tree.setCellRenderer(new NodeCellRenderer(bc, this));
    tree.addTreeSelectionListener(this);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);

    JScrollPane treeView = new JScrollPane(tree);
    add(createButtonPanel(), BorderLayout.NORTH);
    add(treeView, BorderLayout.CENTER);
  }

  public void runDiscovery(){
    Gateway[] g = Gateway.newGateways(bc);
    for (int i = 0 ; i < g.length ; i++) {
      this.createTreeNode(g[i]);
    }
    (new Thread(this.pt)).start();
    tree.expandPath(new TreePath(((DefaultMutableTreeNode)(dtm.getRoot())).getPath())); // expand root node
  }

  //////////////////////////////////////////////////
  //          PropertyChangeListener              //
  //////////////////////////////////////////////////
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(CommonPlugin.COMMON_PLUGIN_ADDED)) {
      Enumeration enu = top.breadthFirstEnumeration();
      enu.nextElement(); // Skip top node
      while ( enu.hasMoreElements() ) {
        // Common plugin added after a gateway connection so firePCE(Plugin.NEW_NODE_CONNECTION, connString , mbsc) again :
	Gateway g = (Gateway) ((DefaultMutableTreeNode) enu.nextElement()).getUserObject();
	if ( g.isConnected() ) {
          activator.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, g.toString(), g.getMbsc());
        }
      }
    }
  }

  //////////////////////////////////////////////////////
  //               TreeSelectionListener              //
  //////////////////////////////////////////////////////
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
    if (node!=null) {
      Object selected = node.getUserObject();
      if ( !selected.equals(NodesTree.TOP_NAME) ) {
        Gateway g = (Gateway) selected;
        Object mbsc = g.getMbsc();
        if ( mbsc != null ){
          activator.firePropertyChangedEvent(Plugin.NEW_NODE_SELECTED, g.toString(), mbsc);
        } else {
          activator.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
        }
      }
    }
  }

  ////////////////////////////////////////////////////
  //            NotificationListener                //
  ////////////////////////////////////////////////////
  public void handleNotification(Notification notification, Object handback) {
    //System.out.println("Connection ==> "+notification.getMessage()+":"+((JMXConnectionNotification)notification).getConnectionId()+":"+handback);
    if ( !JMXConnectionNotification.OPENED.equals(notification.getType()) ){
      Enumeration enu = top.breadthFirstEnumeration();
      enu.nextElement(); // Skip top node
      while ( enu.hasMoreElements() ) {
        Object o = ((DefaultMutableTreeNode) enu.nextElement()).getUserObject();
	Gateway g = (Gateway) o;
	if ( g.toString().equals(handback.toString()) ) {
	  if ( g.isConnected() ) {
	    g.disconnect(this);
	    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent(); 
	    if ( selectedNode != null ) {
	      Gateway g_selected = (Gateway) selectedNode.getUserObject();
	      if ( g_selected.toString().equals(g.toString()) ) {
	        activator.firePropertyChangedEvent(Plugin.EMPTY_NODE, null, "null");
	      }
	    }
            tree.treeDidChange();
            isAllNodesConnected = false;
	  }
	}
      }
    }
  }
  
  ////////////////////////////////////////////////////
  //            Friend methods                      //
  ////////////////////////////////////////////////////
  void stop() {
    this.cont = false;
  }

  public void actionPerformed(ActionEvent ae) {
    Object object = ae.getSource();
    if ( object == jb_addNode ) { // Add a new node into tree
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
      Gateway selectedGateway = null;
      if ( node != null && node != top) {
        selectedGateway = (Gateway) node.getUserObject();
      }
      Gateway newGateway = null;
      try {
        newGateway = Gateway.newGateway(selectedGateway);
      } catch (Exception exep) {
        JOptionPane.showMessageDialog(null, "Gateway creation error:\n "+exep.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }
      if ( newGateway != null ) {
        TreePath tp = tree.getSelectionPath();
        this.createTreeNode(newGateway);
        dtm.reload(top);
        isAllNodesConnected = false;
        tree.setSelectionPath(tp);
      }
    } else if ( object == jb_removeNode ) { // Remove a node from tree
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
      if ( node != top) {
        Gateway g = (Gateway) node.getUserObject();
        if ( !node.equals(top) ){
	  if( JOptionPane.showConfirmDialog(null, "Sure we remove this gateway \""+g.getNickname()+"\" ?\n "+g.toString()) == JOptionPane.YES_OPTION ) {
            g.disconnect(this);
	    dtm.removeNodeFromParent(node);
	    System.out.println("Remove node : "+g);
          }
	}
      }
    } else if ( object == jtf_pool) {
      try {
        POOLING_TIME = Integer.parseInt(jtf_pool.getText());
	if ( POOLING_TIME > 0 & POOLING_TIME < 500 ) {
          POOLING_TIME = 500;
	} else if ( POOLING_TIME < 0 ) {
	  POOLING_TIME = 0;
	} else if ( POOLING_TIME > 9999 ) {
          POOLING_TIME = 9999;
	}
      } catch (Exception exep) {
        //
      }
      if ( POOLING_TIME >= 500 ) {
        jb_refresh.setEnabled(false);
      } else {
        jb_refresh.setEnabled(true);
      }
      jtf_pool.setText(""+POOLING_TIME);
    } else if ( object == jb_refresh) {
      tryToConnectAllNodes();
    }
  }

  ///////////////////////////////////////////////////
  //           Private part                        //
  ///////////////////////////////////////////////////
  private void tryToConnectAllNodes(){
    isAllNodesConnected = true;
    Enumeration enu = top.breadthFirstEnumeration();
    enu.nextElement(); // Skip top node
    while ( enu.hasMoreElements() ) {
      Gateway g = (Gateway) (((DefaultMutableTreeNode) enu.nextElement()).getUserObject());
      if ( !g.isConnected() ) {
        if ( !g.connect(this) ) {
          isAllNodesConnected = false;
        } else {
          tree.treeDidChange();
          activator.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, g.toString(), g.getMbsc());
          // If new connected is the selected one then create a false valueChanged in order to load the NodePanel :
          DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
          if ( selected_node != null &&  !selected_node.equals(top) ) {
            Gateway g_selected = (Gateway) (selected_node.getUserObject());
            if ( g_selected == g ) {
	      this.valueChanged(null);
	    } else {
	    }
	  }
	}
      }
    }
  }

  private class PoolingThread implements Runnable {
    public void run() {
      while ( cont ) {
        if ( !isAllNodesConnected && POOLING_TIME > 0 ) {
	  tryToConnectAllNodes();
        } 
	try {
          Thread.sleep(POOLING_TIME);
        } catch(InterruptedException e) {
          //e.printStackTrace();
        }
      }
    }
  }

  protected static String getProtocolPackageProvider(String protoName) {
    Object o = PROTOCOL_PACKAGE_PROVIDER.get(protoName);
    if ( o != null) {
      return (String) o;
    } else {
      String packages = bc.getProperty("mosgi.jmxconsole.protocol."+protoName+".package");
      if ( packages == null ) {
        packages = "";
      } else {
        System.out.println("Protocol provider package for \""+protoName+"\" is prefixed with \""+packages+"\"");
      }
      PROTOCOL_PACKAGE_PROVIDER.put(protoName, packages);
      return packages;
    }
  }

  private void createTreeNode(Gateway g) {
    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(g);
    DefaultMutableTreeNode parentNode = null;
    Gateway parent = g.getParent();
    if ( parent != null ) {
      Enumeration enu = top.breadthFirstEnumeration();
      while ( enu.hasMoreElements() ) {
        DefaultMutableTreeNode dmtn_tmp = (DefaultMutableTreeNode) enu.nextElement();
        if ( dmtn_tmp.getUserObject().equals(parent) ) {
          parentNode = dmtn_tmp;
          break;
        }
      }
    }
    if ( parentNode == null ) { parentNode = top; }
    parentNode.add(dmtn);
  }

  private JPanel createButtonPanel(){
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    //border...
    buttonPanel.add(Box.createHorizontalGlue());
    jb_addNode = new MyButton('a', "ADD.gif", "Add a gateway", this, buttonPanel);
    buttonPanel.add(Box.createRigidArea(new Dimension(2,0)));
    jb_removeNode = new MyButton('d', "REMOVE.gif", "Remove selected gateway", this, buttonPanel);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(Box.createRigidArea(new Dimension(2,0)));
    buttonPanel.add(Box.createHorizontalGlue());
    jtf_pool = new JTextField(""+POOLING_TIME);
    jtf_pool.addActionListener(this);
    jtf_pool.setToolTipText("<html>Pooling time in millisecond<br>Value range is 500 to 9999<br>A value of 0 means no refresh.</html>");
    jtf_pool.setPreferredSize(new Dimension(40,21));
    jtf_pool.setMaximumSize(new Dimension(40,21));
    buttonPanel.add(jtf_pool);
    buttonPanel.add(Box.createRigidArea(new Dimension(2,0)));
    jb_refresh = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage(bc.getBundle().getResource("images/"+"REFRESH.gif"))));
    jb_refresh.setOpaque(true);
    jb_refresh.setToolTipText("<html>Try to connect gateways now.<br>Enable only if there is no pooling time.</html>");
    jb_refresh.addActionListener(this);
    jb_refresh.setPreferredSize(new Dimension(18, 18));
    jb_refresh.setEnabled(false);
    buttonPanel.add(jb_refresh);
    buttonPanel.add(Box.createHorizontalGlue());
    return buttonPanel;
  }

  static class MyButton extends JButton {
    private final int W = 18, H = 18;
    public MyButton(char c, String iconName, String ttt, NodesTree listener, JPanel panel) {
      super(new ImageIcon(Toolkit.getDefaultToolkit().getImage(bc.getBundle().getResource("images/"+iconName))));
      super.setMnemonic(c);
      super.setToolTipText(ttt);
      super.setBorder(null);
      super.setPreferredSize(new Dimension(W, H));
      super.addActionListener(listener);
      panel.add(this);
    }
  }

}
