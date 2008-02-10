/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.junit4osgi.command;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import junit.framework.AssertionFailedError;
import junit.framework.Test;

/**
 * Result Table Model.
 * Store the results of executed tests.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResultTableModel extends AbstractTableModel{
    
    private static final long serialVersionUID = 1L;
    private List<TestRecord> results = new ArrayList<TestRecord>();

    public int getRowCount() {
        return results.size();
    }

    public int getColumnCount() {
        return 2;
    }
    
    public void addTest(Test t, AssertionFailedError e) {
        TestRecord rec = new TestRecord(t, e);
        results.add(rec);
        fireTableDataChanged();
    }
    
    public void addTest(Test t, Throwable e) {
        TestRecord rec = new TestRecord(t, e);
        results.add(rec);
        fireTableDataChanged();
    }
    
    public void addTest(Test t) {
        TestRecord rec = new TestRecord(t);
        results.add(rec);
        fireTableDataChanged();
    }
    
    public void clear() {
        results.clear();
        fireTableDataChanged();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return results.get(rowIndex).m_test;
        }
        if (columnIndex == 1) {
            TestRecord tr = results.get(rowIndex);
            if (tr.m_wasSucessFull) { return "sucess"; }
            if (tr.m_failure != null) { return "failure"; }
            if (tr.m_error != null) { return "error"; }
        }
        return null;
    }
    
    
    
    public String getColumnName(int column) {
        if (column == 0) {
            return "Test";
        }
        
        if (column == 1) {
            return "Status";
        }
        
        return null;
    }
    
    
    public String getMessage(int row, int column) {
        if (row == -1) {
            return null;
        }
        TestRecord rec = results.get(row);
        if (rec.m_wasSucessFull) {
            return "The test " + rec.m_test + " was executed sucessfully.";
        }
        if (rec.m_failure != null) {
            return "The test " + rec.m_test + " has failed : \n" + rec.m_failure.getMessage();
        }
        if (rec.m_error != null) {
            return "The test " + rec.m_test + " has thrown an error : \n" + rec.m_error.getMessage();
        }
        return "";
    }
    
    private class TestRecord {
        private boolean m_wasSucessFull;
        private Test m_test;
        private AssertionFailedError m_failure;
        private Throwable m_error;
        
        public TestRecord(Test t, AssertionFailedError e) {
            m_test = t;
            m_wasSucessFull = false;
            m_failure = e;
        }
        
        public TestRecord(Test t, Throwable e) {
            m_test = t;
            m_wasSucessFull = false;
            m_error = e;
        }
        
        public TestRecord(Test t) {
            m_test = t;
            m_wasSucessFull = true;
        }
    }

}
