/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrefine.ProjectManager;
import org.openrefine.history.Change;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class MassRowColumnChange implements Change {
    final protected List<ColumnMetadata>    _newColumns;
    final protected List<Row>       _newRows;
    protected List<ColumnMetadata>          _oldColumns;
    protected List<Row>             _oldRows;
    
    public MassRowColumnChange(List<ColumnMetadata> newColumns, List<Row> newRows) {
        _newColumns = newColumns;
        _newRows = newRows;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_oldColumns == null) {
                _oldColumns = new ArrayList<ColumnMetadata>(project.columnModel.getColumns());
            }
            if (_oldRows == null) {
                _oldRows = new ArrayList<Row>(project.rows);
            }
            
            project.columnModel.getColumns().clear();
            project.columnModel.getColumns().addAll(_newColumns);
            
            project.rows.clear();
            project.rows.addAll(_newRows);
            
            ProjectManager.singleton.getInterProjectModel().flushJoinsInvolvingProject(project.id);
            
            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.getColumns().clear();
            project.columnModel.getColumns().addAll(_oldColumns);
            
            project.rows.clear();
            project.rows.addAll(_oldRows);
            
            ProjectManager.singleton.getInterProjectModel().flushJoinsInvolvingProject(project.id);
            
            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newColumnCount="); writer.write(Integer.toString(_newColumns.size())); writer.write('\n');
        for (ColumnMetadata column : _newColumns) {
            column.save(writer);
            writer.write('\n');
        }
        writer.write("oldColumnCount="); writer.write(Integer.toString(_oldColumns.size())); writer.write('\n');
        for (ColumnMetadata column : _oldColumns) {
            column.save(writer);
            writer.write('\n');
        }
        writer.write("newRowCount="); writer.write(Integer.toString(_newRows.size())); writer.write('\n');
        for (Row row : _newRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("oldRowCount="); writer.write(Integer.toString(_oldRows.size())); writer.write('\n');
        for (Row row : _oldRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader) throws Exception {
        List<ColumnMetadata> oldColumns = null;
        List<ColumnMetadata> newColumns = null;

        List<Row> oldRows = null;
        List<Row> newRows = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("oldRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                oldRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldRows.add(Row.load(line));
                    }
                }
            } else if ("newRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                newRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newRows.add(Row.load(line));
                    }
                }
            } else if ("oldColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                oldColumns = new ArrayList<ColumnMetadata>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldColumns.add(ColumnMetadata.load(line));
                    }
                }
            } else if ("newColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                newColumns = new ArrayList<ColumnMetadata>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newColumns.add(ColumnMetadata.load(line));
                    }
                }
            }
        }
        
        MassRowColumnChange change = new MassRowColumnChange(newColumns, newRows);
        change._oldColumns = oldColumns;
        change._oldRows = oldRows;

        
        return change;
    }
}