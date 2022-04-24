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

package com.google.refine.exporters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class TsvExporterTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    StringWriter writer;
    Project project;
    Engine engine;
    Properties options;

    // System Under Test
    CsvExporter SUT;

    @BeforeMethod
    public void SetUp() {
        SUT = new CsvExporter('\t');// new TsvExporter();
        writer = new StringWriter();
        project = new Project();
        engine = new Engine(project);
        options = mock(Properties.class);
    }

    @AfterMethod
    public void TearDown() {
        SUT = null;
        writer = null;
        project = null;
        engine = null;
        options = null;
    }

    @Test
    public void exportSimpleTsv() {
        CreateGrid(2, 2);

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\n" +
                "row0cell0\trow0cell1\n" +
                "row1cell0\trow1cell1\n");

    }

    @Test
    public void exportSimpleTsvNoHeader() {
        CreateGrid(2, 2);
        when(options.getProperty("printColumnHeader")).thenReturn("false");
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "row0cell0\trow0cell1\n" +
                "row1cell0\trow1cell1\n");

        verify(options, times(2)).getProperty("printColumnHeader");
    }

    @Test
    public void exportTsvWithLineBreaks() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line\n\n\nbreak", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"line\n\n\nbreak\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithComma() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("with , Comma", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\twith , Comma\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("line has \"quote\"", null));
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"line has \"quote\"\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEmptyCells() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, null);
        project.rows.get(2).cells.set(0, null);
        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\trow1cell2\n" +
                "\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEnclosedQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("\"cell enclosed by (\") quote character\"", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"\"cell enclosed by (\") quote character\"\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithDQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (\") double quote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (\") double quote character\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithStartQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("\"cell starting with a double quote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"\"cell starting with a double quote character\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEndQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell ending with a double quote character\"", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell ending with a double quote character\"\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithQuoteNTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (\") double quote and (\t) tab character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (\") double quote and (\t) tab character\"\trow1cell2\n"
                + "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithQuoteNStartingTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("\tcell containing (\") double quote and starting with (\t) tab character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"\tcell containing (\") double quote and starting with (\t) tab character\"\trow1cell2\n"
                + "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithEnclosedTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("\tcell enclosed by (\t) tab character\t", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"\tcell enclosed by (\t) tab character\t\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (\t) tab character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (\t) tab character\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithStartTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("\tcell starting with a tab character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"\tcell starting with a tab character\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEndTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell ending with a tab character\t", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell ending with a tab character\t\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithNewlineNTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (\n) new line and (\t) tab character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (\n) new line and (\t) tab character\"\trow1cell2\n"
                + "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithTabEndingNewLine() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (\t) tab character and ending with new line (\n) character\n", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (\t) tab character and ending with new line (\n) character\n\"\trow1cell2\n"
                + "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithEnclosedSingleQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("‘cell enclosed by (') single quote character’", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t‘cell enclosed by (') single quote character’\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithSingleQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (') single quote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell containing (') single quote character\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithStartSingleQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("‘cell starting with a single quote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t‘cell starting with a single quote character\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEndSingleQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell ending with a single quote character’", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell ending with a single quote character’\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithSQuoteNBQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (‘) single quote and (`) backquote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell containing (‘) single quote and (`) backquote character\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithSingleQuoteNStartingTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (‘) single quote and ending with (\t) tab character\t", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (‘) single quote and ending with (\t) tab character\t\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithEnclosedBackQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("`cell enclosed by (`) back quote character`", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t`cell enclosed by (`) back quote character`\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithBackQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (`) backquote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell containing (`) backquote character\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithStartBackQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("`cell starting with a back quote character", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t`cell starting with a back quote character\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithEndBackQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell ending with a back quote character`", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell ending with a back quote character`\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithBackQuoteNTab() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (`) back quote and ending with (\t) tab character\t", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (`) back quote and ending with (\t) tab character\t\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");

    }

    @Test
    public void exportTsvWithBackQuoteNDoubleQuote() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (`) back quote and ending with (\") double quote character\"", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\t\"cell containing (`) back quote and ending with (\") double quote character\"\"\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    @Test
    public void exportTsvWithBackQuoteNComma() {
        CreateGrid(3, 3);

        project.rows.get(1).cells.set(1, new Cell("cell containing (`) back quote and ending with (,) comma,", null));

        try {
            SUT.export(project, options, engine, writer);
        } catch (IOException e) {
            Assert.fail();
        }

        Assert.assertEquals(writer.toString(), "column0\tcolumn1\tcolumn2\n" +
                "row0cell0\trow0cell1\trow0cell2\n" +
                "row1cell0\tcell containing (`) back quote and ending with (,) comma,\trow1cell2\n" +
                "row2cell0\trow2cell1\trow2cell2\n");
    }

    // helper methods

    protected void CreateColumns(int noOfColumns) {
        for (int i = 0; i < noOfColumns; i++) {
            try {
                project.columnModel.addColumn(i, new Column(i, "column" + i), true);
                project.columnModel.columns.get(i).getCellIndex();
            } catch (ModelException e1) {
                Assert.fail("Could not create column");
            }
        }
    }

    protected void CreateGrid(int noOfRows, int noOfColumns) {
        CreateColumns(noOfColumns);

        for (int i = 0; i < noOfRows; i++) {
            Row row = new Row(noOfColumns);
            for (int j = 0; j < noOfColumns; j++) {
                row.cells.add(new Cell("row" + i + "cell" + j, null));
            }
            project.rows.add(row);
        }
    }
}
