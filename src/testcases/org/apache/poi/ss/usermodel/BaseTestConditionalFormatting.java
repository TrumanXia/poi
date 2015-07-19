/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.ss.usermodel;

import junit.framework.TestCase;

import org.apache.poi.hssf.usermodel.HSSFConditionalFormatting;
import org.apache.poi.hssf.usermodel.HSSFConditionalFormattingRule;
import org.apache.poi.ss.ITestDataProvider;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType;
import org.apache.poi.ss.usermodel.IconMultiStateFormatting.IconSet;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Base tests for Conditional Formatting, for both HSSF and XSSF
 */
public abstract class BaseTestConditionalFormatting extends TestCase {
    private final ITestDataProvider _testDataProvider;

    public BaseTestConditionalFormatting(ITestDataProvider testDataProvider){
        _testDataProvider = testDataProvider;
    }
    
    protected abstract void assertColour(String hexExpected, Color actual);

    public void testBasic() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sh = wb.createSheet();
        SheetConditionalFormatting sheetCF = sh.getSheetConditionalFormatting();

        assertEquals(0, sheetCF.getNumConditionalFormattings());
        try {
            assertNull(sheetCF.getConditionalFormattingAt(0));
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Specified CF index 0 is outside the allowable range"));
        }

        try {
            sheetCF.removeConditionalFormatting(0);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Specified CF index 0 is outside the allowable range"));
        }

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule("1");
        ConditionalFormattingRule rule2 = sheetCF.createConditionalFormattingRule("2");
        ConditionalFormattingRule rule3 = sheetCF.createConditionalFormattingRule("3");
        ConditionalFormattingRule rule4 = sheetCF.createConditionalFormattingRule("4");
        try {
            sheetCF.addConditionalFormatting(null, rule1);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("regions must not be null"));
        }
        try {
            sheetCF.addConditionalFormatting(
                    new CellRangeAddress[]{ CellRangeAddress.valueOf("A1:A3") },
                    (ConditionalFormattingRule)null);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("cfRules must not be null"));
        }

        try {
            sheetCF.addConditionalFormatting(
                    new CellRangeAddress[]{ CellRangeAddress.valueOf("A1:A3") },
                    new ConditionalFormattingRule[0]);
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("cfRules must not be empty"));
        }

        try {
            sheetCF.addConditionalFormatting(
                    new CellRangeAddress[]{ CellRangeAddress.valueOf("A1:A3") },
                    new ConditionalFormattingRule[]{rule1, rule2, rule3, rule4});
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Number of rules must not exceed 3"));
        }
    }

    /**
     * Test format conditions based on a boolean formula
     */
    public void testBooleanFormulaConditions() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sh = wb.createSheet();
        SheetConditionalFormatting sheetCF = sh.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule("SUM(A1:A5)>10");
        assertEquals(ConditionType.FORMULA.id, rule1.getConditionType());
        assertEquals(ConditionType.FORMULA, rule1.getConditionTypeType());
        assertEquals("SUM(A1:A5)>10", rule1.getFormula1());
        int formatIndex1 = sheetCF.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("B1"),
                        CellRangeAddress.valueOf("C3"),
                }, rule1);
        assertEquals(0, formatIndex1);
        assertEquals(1, sheetCF.getNumConditionalFormattings());
        CellRangeAddress[] ranges1 = sheetCF.getConditionalFormattingAt(formatIndex1).getFormattingRanges();
        assertEquals(2, ranges1.length);
        assertEquals("B1", ranges1[0].formatAsString());
        assertEquals("C3", ranges1[1].formatAsString());

        // adjacent address are merged
        int formatIndex2 = sheetCF.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("B1"),
                        CellRangeAddress.valueOf("B2"),
                        CellRangeAddress.valueOf("B3"),
                }, rule1);
        assertEquals(1, formatIndex2);
        assertEquals(2, sheetCF.getNumConditionalFormattings());
        CellRangeAddress[] ranges2 = sheetCF.getConditionalFormattingAt(formatIndex2).getFormattingRanges();
        assertEquals(1, ranges2.length);
        assertEquals("B1:B3", ranges2[0].formatAsString());
    }

    public void testSingleFormulaConditions() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sh = wb.createSheet();
        SheetConditionalFormatting sheetCF = sh.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.EQUAL, "SUM(A1:A5)+10");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule1.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule1.getConditionTypeType());
        assertEquals("SUM(A1:A5)+10", rule1.getFormula1());
        assertEquals(ComparisonOperator.EQUAL, rule1.getComparisonOperation());

        ConditionalFormattingRule rule2 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.NOT_EQUAL, "15");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule2.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule2.getConditionTypeType());
        assertEquals("15", rule2.getFormula1());
        assertEquals(ComparisonOperator.NOT_EQUAL, rule2.getComparisonOperation());

        ConditionalFormattingRule rule3 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.NOT_EQUAL, "15");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule3.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule3.getConditionTypeType());
        assertEquals("15", rule3.getFormula1());
        assertEquals(ComparisonOperator.NOT_EQUAL, rule3.getComparisonOperation());

        ConditionalFormattingRule rule4 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.GT, "0");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule4.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule4.getConditionTypeType());
        assertEquals("0", rule4.getFormula1());
        assertEquals(ComparisonOperator.GT, rule4.getComparisonOperation());

        ConditionalFormattingRule rule5 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.LT, "0");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule5.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule5.getConditionTypeType());
        assertEquals("0", rule5.getFormula1());
        assertEquals(ComparisonOperator.LT, rule5.getComparisonOperation());

        ConditionalFormattingRule rule6 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.GE, "0");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule6.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule6.getConditionTypeType());
        assertEquals("0", rule6.getFormula1());
        assertEquals(ComparisonOperator.GE, rule6.getComparisonOperation());

        ConditionalFormattingRule rule7 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.LE, "0");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule7.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule7.getConditionTypeType());
        assertEquals("0", rule7.getFormula1());
        assertEquals(ComparisonOperator.LE, rule7.getComparisonOperation());

        ConditionalFormattingRule rule8 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.BETWEEN, "0", "5");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule8.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule8.getConditionTypeType());
        assertEquals("0", rule8.getFormula1());
        assertEquals("5", rule8.getFormula2());
        assertEquals(ComparisonOperator.BETWEEN, rule8.getComparisonOperation());

        ConditionalFormattingRule rule9 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.NOT_BETWEEN, "0", "5");
        assertEquals(ConditionType.CELL_VALUE_IS.id, rule9.getConditionType());
        assertEquals(ConditionType.CELL_VALUE_IS, rule9.getConditionTypeType());
        assertEquals("0", rule9.getFormula1());
        assertEquals("5", rule9.getFormula2());
        assertEquals(ComparisonOperator.NOT_BETWEEN, rule9.getComparisonOperation());
    }

    public void testCopy() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sheet1 = wb.createSheet();
        Sheet sheet2 = wb.createSheet();
        SheetConditionalFormatting sheet1CF = sheet1.getSheetConditionalFormatting();
        SheetConditionalFormatting sheet2CF = sheet2.getSheetConditionalFormatting();
        assertEquals(0, sheet1CF.getNumConditionalFormattings());
        assertEquals(0, sheet2CF.getNumConditionalFormattings());

        ConditionalFormattingRule rule1 = sheet1CF.createConditionalFormattingRule(
                ComparisonOperator.EQUAL, "SUM(A1:A5)+10");

        ConditionalFormattingRule rule2 = sheet1CF.createConditionalFormattingRule(
                ComparisonOperator.NOT_EQUAL, "15");

        // adjacent address are merged
        int formatIndex = sheet1CF.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("A1:A5"),
                        CellRangeAddress.valueOf("C1:C5")
                }, rule1, rule2);
        assertEquals(0, formatIndex);
        assertEquals(1, sheet1CF.getNumConditionalFormattings());

        assertEquals(0, sheet2CF.getNumConditionalFormattings());
        sheet2CF.addConditionalFormatting(sheet1CF.getConditionalFormattingAt(formatIndex));
        assertEquals(1, sheet2CF.getNumConditionalFormattings());

        ConditionalFormatting sheet2cf = sheet2CF.getConditionalFormattingAt(0);
        assertEquals(2, sheet2cf.getNumberOfRules());
        assertEquals("SUM(A1:A5)+10", sheet2cf.getRule(0).getFormula1());
        assertEquals(ComparisonOperator.EQUAL, sheet2cf.getRule(0).getComparisonOperation());
        assertEquals(ConditionalFormattingRule.CONDITION_TYPE_CELL_VALUE_IS, sheet2cf.getRule(0).getConditionType());
        assertEquals("15", sheet2cf.getRule(1).getFormula1());
        assertEquals(ComparisonOperator.NOT_EQUAL, sheet2cf.getRule(1).getComparisonOperation());
        assertEquals(ConditionalFormattingRule.CONDITION_TYPE_CELL_VALUE_IS, sheet2cf.getRule(1).getConditionType());
    }

    public void testRemove() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sheet1 = wb.createSheet();
        SheetConditionalFormatting sheetCF = sheet1.getSheetConditionalFormatting();
        assertEquals(0, sheetCF.getNumConditionalFormattings());

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.EQUAL, "SUM(A1:A5)");

        // adjacent address are merged
        int formatIndex = sheetCF.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("A1:A5")
                }, rule1);
        assertEquals(0, formatIndex);
        assertEquals(1, sheetCF.getNumConditionalFormattings());
        sheetCF.removeConditionalFormatting(0);
        assertEquals(0, sheetCF.getNumConditionalFormattings());
        try {
            assertNull(sheetCF.getConditionalFormattingAt(0));
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Specified CF index 0 is outside the allowable range"));
        }

        formatIndex = sheetCF.addConditionalFormatting(
                new CellRangeAddress[]{
                        CellRangeAddress.valueOf("A1:A5")
                }, rule1);
        assertEquals(0, formatIndex);
        assertEquals(1, sheetCF.getNumConditionalFormattings());
        sheetCF.removeConditionalFormatting(0);
        assertEquals(0, sheetCF.getNumConditionalFormattings());
        try {
            assertNull(sheetCF.getConditionalFormattingAt(0));
            fail("expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Specified CF index 0 is outside the allowable range"));
        }
    }
    
    public void testCreateCF() {
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();
        String formula = "7";

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(formula);
        FontFormatting fontFmt = rule1.createFontFormatting();
        fontFmt.setFontStyle(true, false);

        BorderFormatting bordFmt = rule1.createBorderFormatting();
        bordFmt.setBorderBottom(BorderFormatting.BORDER_THIN);
        bordFmt.setBorderTop(BorderFormatting.BORDER_THICK);
        bordFmt.setBorderLeft(BorderFormatting.BORDER_DASHED);
        bordFmt.setBorderRight(BorderFormatting.BORDER_DOTTED);

        PatternFormatting patternFmt = rule1.createPatternFormatting();
        patternFmt.setFillBackgroundColor(IndexedColors.YELLOW.index);


        ConditionalFormattingRule rule2 = sheetCF.createConditionalFormattingRule(ComparisonOperator.BETWEEN, "1", "2");
        ConditionalFormattingRule [] cfRules =
        {
            rule1, rule2
        };

        short col = 1;
        CellRangeAddress [] regions = {
            new CellRangeAddress(0, 65535, col, col)
        };

        sheetCF.addConditionalFormatting(regions, cfRules);
        sheetCF.addConditionalFormatting(regions, cfRules);

        // Verification
        assertEquals(2, sheetCF.getNumConditionalFormattings());
        sheetCF.removeConditionalFormatting(1);
        assertEquals(1, sheetCF.getNumConditionalFormattings());
        ConditionalFormatting cf = sheetCF.getConditionalFormattingAt(0);
        assertNotNull(cf);

        regions = cf.getFormattingRanges();
        assertNotNull(regions);
        assertEquals(1, regions.length);
        CellRangeAddress r = regions[0];
        assertEquals(1, r.getFirstColumn());
        assertEquals(1, r.getLastColumn());
        assertEquals(0, r.getFirstRow());
        assertEquals(65535, r.getLastRow());

        assertEquals(2, cf.getNumberOfRules());

        rule1 = cf.getRule(0);
        assertEquals("7",rule1.getFormula1());
        assertNull(rule1.getFormula2());

        FontFormatting    r1fp = rule1.getFontFormatting();
        assertNotNull(r1fp);

        assertTrue(r1fp.isItalic());
        assertFalse(r1fp.isBold());

        BorderFormatting  r1bf = rule1.getBorderFormatting();
        assertNotNull(r1bf);
        assertEquals(BorderFormatting.BORDER_THIN, r1bf.getBorderBottom());
        assertEquals(BorderFormatting.BORDER_THICK,r1bf.getBorderTop());
        assertEquals(BorderFormatting.BORDER_DASHED,r1bf.getBorderLeft());
        assertEquals(BorderFormatting.BORDER_DOTTED,r1bf.getBorderRight());

        PatternFormatting r1pf = rule1.getPatternFormatting();
        assertNotNull(r1pf);
//        assertEquals(IndexedColors.YELLOW.index,r1pf.getFillBackgroundColor());

        rule2 = cf.getRule(1);
        assertEquals("2",rule2.getFormula2());
        assertEquals("1",rule2.getFormula1());
    }

    public void testClone() {

        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sheet = wb.createSheet();
        String formula = "7";

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(formula);
        FontFormatting fontFmt = rule1.createFontFormatting();
        fontFmt.setFontStyle(true, false);

        PatternFormatting patternFmt = rule1.createPatternFormatting();
        patternFmt.setFillBackgroundColor(IndexedColors.YELLOW.index);


        ConditionalFormattingRule rule2 = sheetCF.createConditionalFormattingRule(ComparisonOperator.BETWEEN, "1", "2");
        ConditionalFormattingRule [] cfRules =
        {
            rule1, rule2
        };

        short col = 1;
        CellRangeAddress [] regions = {
            new CellRangeAddress(0, 65535, col, col)
        };

        sheetCF.addConditionalFormatting(regions, cfRules);

        try {
            wb.cloneSheet(0);
        } catch (RuntimeException e) {
            if (e.getMessage().indexOf("needs to define a clone method") > 0) {
                fail("Indentified bug 45682");
            }
            throw e;
        }
        assertEquals(2, wb.getNumberOfSheets());
    }

    public void testShiftRows() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sheet = wb.createSheet();

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.BETWEEN, "SUM(A10:A15)", "1+SUM(B16:B30)");
        FontFormatting fontFmt = rule1.createFontFormatting();
        fontFmt.setFontStyle(true, false);

        PatternFormatting patternFmt = rule1.createPatternFormatting();
        patternFmt.setFillBackgroundColor(IndexedColors.YELLOW.index);

        ConditionalFormattingRule rule2 = sheetCF.createConditionalFormattingRule(
                ComparisonOperator.BETWEEN, "SUM(A10:A15)", "1+SUM(B16:B30)");
        BorderFormatting borderFmt = rule2.createBorderFormatting();
        borderFmt.setBorderDiagonal((short) 2);

        CellRangeAddress [] regions = {
            new CellRangeAddress(2, 4, 0, 0), // A3:A5
        };
        sheetCF.addConditionalFormatting(regions, rule1);
        sheetCF.addConditionalFormatting(regions, rule2);

        // This row-shift should destroy the CF region
        sheet.shiftRows(10, 20, -9);
        assertEquals(0, sheetCF.getNumConditionalFormattings());

        // re-add the CF
        sheetCF.addConditionalFormatting(regions, rule1);
        sheetCF.addConditionalFormatting(regions, rule2);

        // This row shift should only affect the formulas
        sheet.shiftRows(14, 17, 8);
        ConditionalFormatting cf1 = sheetCF.getConditionalFormattingAt(0);
        assertEquals("SUM(A10:A23)", cf1.getRule(0).getFormula1());
        assertEquals("1+SUM(B24:B30)", cf1.getRule(0).getFormula2());
        ConditionalFormatting cf2 = sheetCF.getConditionalFormattingAt(1);
        assertEquals("SUM(A10:A23)", cf2.getRule(0).getFormula1());
        assertEquals("1+SUM(B24:B30)", cf2.getRule(0).getFormula2());

        sheet.shiftRows(0, 8, 21);
        cf1 = sheetCF.getConditionalFormattingAt(0);
        assertEquals("SUM(A10:A21)", cf1.getRule(0).getFormula1());
        assertEquals("1+SUM(#REF!)", cf1.getRule(0).getFormula2());
        cf2 = sheetCF.getConditionalFormattingAt(1);
        assertEquals("SUM(A10:A21)", cf2.getRule(0).getFormula1());
        assertEquals("1+SUM(#REF!)", cf2.getRule(0).getFormula2());
    }

    protected void testRead(String filename){
        Workbook wb = _testDataProvider.openSampleWorkbook(filename);
        Sheet sh = wb.getSheet("CF");
        SheetConditionalFormatting sheetCF = sh.getSheetConditionalFormatting();
        assertEquals(3, sheetCF.getNumConditionalFormattings());

        ConditionalFormatting cf1 = sheetCF.getConditionalFormattingAt(0);
        assertEquals(2, cf1.getNumberOfRules());

        CellRangeAddress[] regions1 = cf1.getFormattingRanges();
        assertEquals(1, regions1.length);
        assertEquals("A1:A8", regions1[0].formatAsString());

        // CF1 has two rules: values less than -3 are bold-italic red, values greater than 3 are green
        ConditionalFormattingRule rule1 = cf1.getRule(0);
        assertEquals(ConditionType.CELL_VALUE_IS, rule1.getConditionTypeType());
        assertEquals(ComparisonOperator.GT, rule1.getComparisonOperation());
        assertEquals("3", rule1.getFormula1());
        assertNull(rule1.getFormula2());
        // fills and borders are not set
        assertNull(rule1.getPatternFormatting());
        assertNull(rule1.getBorderFormatting());

        FontFormatting fmt1 = rule1.getFontFormatting();
//        assertEquals(IndexedColors.GREEN.index, fmt1.getFontColorIndex());
        assertTrue(fmt1.isBold());
        assertFalse(fmt1.isItalic());

        ConditionalFormattingRule rule2 = cf1.getRule(1);
        assertEquals(ConditionType.CELL_VALUE_IS, rule2.getConditionTypeType());
        assertEquals(ComparisonOperator.LT, rule2.getComparisonOperation());
        assertEquals("-3", rule2.getFormula1());
        assertNull(rule2.getFormula2());
        assertNull(rule2.getPatternFormatting());
        assertNull(rule2.getBorderFormatting());

        FontFormatting fmt2 = rule2.getFontFormatting();
//        assertEquals(IndexedColors.RED.index, fmt2.getFontColorIndex());
        assertTrue(fmt2.isBold());
        assertTrue(fmt2.isItalic());


        ConditionalFormatting cf2 = sheetCF.getConditionalFormattingAt(1);
        assertEquals(1, cf2.getNumberOfRules());
        CellRangeAddress[] regions2 = cf2.getFormattingRanges();
        assertEquals(1, regions2.length);
        assertEquals("B9", regions2[0].formatAsString());

        ConditionalFormattingRule rule3 = cf2.getRule(0);
        assertEquals(ConditionType.FORMULA, rule3.getConditionTypeType());
        assertEquals(ComparisonOperator.NO_COMPARISON, rule3.getComparisonOperation());
        assertEquals("$A$8>5", rule3.getFormula1());
        assertNull(rule3.getFormula2());

        FontFormatting fmt3 = rule3.getFontFormatting();
//        assertEquals(IndexedColors.RED.index, fmt3.getFontColorIndex());
        assertTrue(fmt3.isBold());
        assertTrue(fmt3.isItalic());

        PatternFormatting fmt4 = rule3.getPatternFormatting();
//        assertEquals(IndexedColors.LIGHT_CORNFLOWER_BLUE.index, fmt4.getFillBackgroundColor());
//        assertEquals(IndexedColors.AUTOMATIC.index, fmt4.getFillForegroundColor());
        assertEquals(PatternFormatting.NO_FILL, fmt4.getFillPattern());
        // borders are not set
        assertNull(rule3.getBorderFormatting());

        ConditionalFormatting cf3 = sheetCF.getConditionalFormattingAt(2);
        CellRangeAddress[] regions3 = cf3.getFormattingRanges();
        assertEquals(1, regions3.length);
        assertEquals("B1:B7", regions3[0].formatAsString());
        assertEquals(2, cf3.getNumberOfRules());

        ConditionalFormattingRule rule4 = cf3.getRule(0);
        assertEquals(ConditionType.CELL_VALUE_IS, rule4.getConditionTypeType());
        assertEquals(ComparisonOperator.LE, rule4.getComparisonOperation());
        assertEquals("\"AAA\"", rule4.getFormula1());
        assertNull(rule4.getFormula2());

        ConditionalFormattingRule rule5 = cf3.getRule(1);
        assertEquals(ConditionType.CELL_VALUE_IS, rule5.getConditionTypeType());
        assertEquals(ComparisonOperator.BETWEEN, rule5.getComparisonOperation());
        assertEquals("\"A\"", rule5.getFormula1());
        assertEquals("\"AAA\"", rule5.getFormula2());
    }

    public void testReadOffice2007(String filename) {
        Workbook wb = _testDataProvider.openSampleWorkbook(filename);
        Sheet s = wb.getSheet("CF");
        ConditionalFormatting cf = null;
        ConditionalFormattingRule cr = null;
        IconMultiStateFormatting icon = null;
        ConditionalFormattingThreshold th = null;
        
        // Sanity check data
        assertEquals("Values", s.getRow(0).getCell(0).toString());
        assertEquals("10.0", s.getRow(2).getCell(0).toString());

        // Check we found all the conditional formattings rules we should have
        SheetConditionalFormatting sheetCF = s.getSheetConditionalFormatting();
        int numCF = 3;
        int numCF12 = 15;
        int numCFEX = 0; // TODO This should be 1, but we don't support CFEX formattings yet
        assertEquals(numCF+numCF12+numCFEX, sheetCF.getNumConditionalFormattings());
        
        int fCF = 0, fCF12 = 0, fCFEX = 0;
        for (int i=0; i<sheetCF.getNumConditionalFormattings(); i++) {
            cf = sheetCF.getConditionalFormattingAt(i);
            if (cf instanceof HSSFConditionalFormatting) {
                String str = cf.toString();
                if (str.contains("[CF]")) fCF++;
                if (str.contains("[CF12]")) fCF12++;
                if (str.contains("[CFEX]")) fCFEX++;
            } else {
                ConditionType type = cf.getRule(cf.getNumberOfRules()-1).getConditionTypeType();
                if (type == ConditionType.CELL_VALUE_IS ||
                    type == ConditionType.FORMULA) {
                    fCF++;
                } else {
                    // TODO Properly detect Ext ones from the xml
                    fCF12++;
                }
            }
        }
        assertEquals(numCF, fCF);
        assertEquals(numCF12, fCF12);
        assertEquals(numCFEX, fCFEX);
        
        
        // Check the rules / values in detail
        
        
        // Highlight Positive values - Column C
        cf = sheetCF.getConditionalFormattingAt(0);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("C2:C17", cf.getFormattingRanges()[0].formatAsString());
        
        assertEquals(1, cf.getNumberOfRules());
        cr = cf.getRule(0);
        assertEquals(ConditionType.CELL_VALUE_IS, cr.getConditionTypeType());
        assertEquals(ComparisonOperator.GT, cr.getComparisonOperation());
        assertEquals("0", cr.getFormula1());
        assertEquals(null, cr.getFormula2());
        // When it matches:
        //   Sets the font colour to dark green
        //   Sets the background colour to lighter green
        // TODO Should the colours be slightly different between formats? Would CFEX support help for HSSF?
        if (cr instanceof HSSFConditionalFormattingRule) {
            assertColour("0:8080:0", cr.getFontFormatting().getFontColor());
            assertColour("CCCC:FFFF:CCCC", cr.getPatternFormatting().getFillBackgroundColorColor());
        } else {
            assertColour("006100", cr.getFontFormatting().getFontColor());
            assertColour("C6EFCE", cr.getPatternFormatting().getFillBackgroundColorColor());
        }
        
        
        // Highlight 10-30 - Column D
        cf = sheetCF.getConditionalFormattingAt(1);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("D2:D17", cf.getFormattingRanges()[0].formatAsString());
        
        assertEquals(1, cf.getNumberOfRules());
        cr = cf.getRule(0);
        assertEquals(ConditionType.CELL_VALUE_IS, cr.getConditionTypeType());
        assertEquals(ComparisonOperator.BETWEEN, cr.getComparisonOperation());
        assertEquals("10", cr.getFormula1());
        assertEquals("30", cr.getFormula2());
        // When it matches:
        //   Sets the font colour to dark red
        //   Sets the background colour to lighter red
        // TODO Should the colours be slightly different between formats? Would CFEX support help for HSSF?
        if (cr instanceof HSSFConditionalFormattingRule) {
            assertColour("8080:0:8080", cr.getFontFormatting().getFontColor());
            assertColour("FFFF:9999:CCCC", cr.getPatternFormatting().getFillBackgroundColorColor());
        } else {
            assertColour("9C0006", cr.getFontFormatting().getFontColor());
            assertColour("FFC7CE", cr.getPatternFormatting().getFillBackgroundColorColor());
        }

        
        // Data Bars - Column E
        cf = sheetCF.getConditionalFormattingAt(2);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("E2:E17", cf.getFormattingRanges()[0].formatAsString());
        
        assertEquals(1, cf.getNumberOfRules());
        cr = cf.getRule(0);
        assertEquals(ConditionType.DATA_BAR, cr.getConditionTypeType());
        // TODO Support Data Bars, then check the rest of this rule
        
        
        // Colours R->G - Column F
        cf = sheetCF.getConditionalFormattingAt(3);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("F2:F17", cf.getFormattingRanges()[0].formatAsString());
        
        assertEquals(1, cf.getNumberOfRules());
        cr = cf.getRule(0);
        assertEquals(ConditionType.COLOR_SCALE, cr.getConditionTypeType());
        // TODO Support Color Scales, then check the rest of this rule

        
        // Colours BWR - Column G
        cf = sheetCF.getConditionalFormattingAt(4);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("G2:G17", cf.getFormattingRanges()[0].formatAsString());
        
        assertEquals(1, cf.getNumberOfRules());
        cr = cf.getRule(0);
        assertEquals(ConditionType.COLOR_SCALE, cr.getConditionTypeType());
        // TODO Support Color Scales, then check the rest of this rule

        
        // Icons : Default - Column H, percentage thresholds
        cf = sheetCF.getConditionalFormattingAt(5);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("H2:H17", cf.getFormattingRanges()[0].formatAsString());
        assertIconSetPercentages(cf, IconSet.GYR_3_TRAFFIC_LIGHTS, 0d, 33d, 67d);
        
        
        // Icons : 3 signs - Column I
        cf = sheetCF.getConditionalFormattingAt(6);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("I2:I17", cf.getFormattingRanges()[0].formatAsString());
        assertIconSetPercentages(cf, IconSet.GYR_3_SHAPES, 0d, 33d, 67d);
        
        
        // Icons : 3 traffic lights 2 - Column J
        cf = sheetCF.getConditionalFormattingAt(7);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("J2:J17", cf.getFormattingRanges()[0].formatAsString());
        assertIconSetPercentages(cf, IconSet.GYR_3_TRAFFIC_LIGHTS_BOX, 0d, 33d, 67d);
        
        
        // Icons : 4 traffic lights - Column K
        cf = sheetCF.getConditionalFormattingAt(8);
        assertEquals(1, cf.getFormattingRanges().length);
        assertEquals("K2:K17", cf.getFormattingRanges()[0].formatAsString());
        assertIconSetPercentages(cf, IconSet.GYRB_4_TRAFFIC_LIGHTS, 0d, 25d, 50d, 75d);

        
        // Icons : 3 symbols - Column L
        // Icons : 3 flags - Column M
        // Icons : 3 symbols 2 - Column N
        // Icons : 3 arrows - Column O     
        // Icons : 5 arrows grey - Column P    
        // Icons : 3 stars (ext) - Column Q
        // Icons : 4 ratings - Column R
        // Icons : 5 ratings - Column S
        // Custom Icon+Format - Column T
        // Mixed icons - Column U

    }
    private void assertIconSetPercentages(ConditionalFormatting cf, IconSet iconset, Double...vals) {
        assertEquals(1, cf.getNumberOfRules());
        ConditionalFormattingRule cr = cf.getRule(0);
        
        assertEquals(ConditionType.ICON_SET, cr.getConditionTypeType());
        assertEquals(ComparisonOperator.NO_COMPARISON, cr.getComparisonOperation());
        assertEquals(null, cr.getFormula1());
        assertEquals(null, cr.getFormula2());
        
        IconMultiStateFormatting icon = cr.getMultiStateFormatting();
        assertNotNull(icon);
        assertEquals(iconset, icon.getIconSet());
        assertEquals(false, icon.isIconOnly());
        assertEquals(false, icon.isReversed());
        
        assertNotNull(icon.getThresholds());
        assertEquals(vals.length, icon.getThresholds().length);
        for (int i=0; i<vals.length; i++) {
            Double v = vals[i];
            ConditionalFormattingThreshold th = icon.getThresholds()[i];
            assertEquals(RangeType.PERCENT, th.getRangeType());
            assertEquals(v, th.getValue());
            assertEquals(null, th.getFormula());
        }
    }

    public void testCreateFontFormatting() {
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "7");
        FontFormatting fontFmt = rule1.createFontFormatting();
        assertFalse(fontFmt.isItalic());
        assertFalse(fontFmt.isBold());
        fontFmt.setFontStyle(true, true);
        assertTrue(fontFmt.isItalic());
        assertTrue(fontFmt.isBold());

        assertEquals(-1, fontFmt.getFontHeight()); // not modified
        fontFmt.setFontHeight(200);
        assertEquals(200, fontFmt.getFontHeight()); 
        fontFmt.setFontHeight(100);
        assertEquals(100, fontFmt.getFontHeight());

        assertEquals(FontFormatting.SS_NONE, fontFmt.getEscapementType());
        fontFmt.setEscapementType(FontFormatting.SS_SUB);
        assertEquals(FontFormatting.SS_SUB, fontFmt.getEscapementType());
        fontFmt.setEscapementType(FontFormatting.SS_NONE);
        assertEquals(FontFormatting.SS_NONE, fontFmt.getEscapementType());
        fontFmt.setEscapementType(FontFormatting.SS_SUPER);
        assertEquals(FontFormatting.SS_SUPER, fontFmt.getEscapementType());

        assertEquals(FontFormatting.U_NONE, fontFmt.getUnderlineType());
        fontFmt.setUnderlineType(FontFormatting.U_SINGLE);
        assertEquals(FontFormatting.U_SINGLE, fontFmt.getUnderlineType());
        fontFmt.setUnderlineType(FontFormatting.U_NONE);
        assertEquals(FontFormatting.U_NONE, fontFmt.getUnderlineType());
        fontFmt.setUnderlineType(FontFormatting.U_DOUBLE);
        assertEquals(FontFormatting.U_DOUBLE, fontFmt.getUnderlineType());

        assertEquals(-1, fontFmt.getFontColorIndex());
        fontFmt.setFontColorIndex(IndexedColors.RED.index);
        assertEquals(IndexedColors.RED.index, fontFmt.getFontColorIndex());
        fontFmt.setFontColorIndex(IndexedColors.AUTOMATIC.index);
        assertEquals(IndexedColors.AUTOMATIC.index, fontFmt.getFontColorIndex());
        fontFmt.setFontColorIndex(IndexedColors.BLUE.index);
        assertEquals(IndexedColors.BLUE.index, fontFmt.getFontColorIndex());

        ConditionalFormattingRule [] cfRules = { rule1 };

        CellRangeAddress [] regions = { CellRangeAddress.valueOf("A1:A5") };

        sheetCF.addConditionalFormatting(regions, cfRules);

        // Verification
        ConditionalFormatting cf = sheetCF.getConditionalFormattingAt(0);
        assertNotNull(cf);

        assertEquals(1, cf.getNumberOfRules());

        FontFormatting  r1fp = cf.getRule(0).getFontFormatting();
        assertNotNull(r1fp);

        assertTrue(r1fp.isItalic());
        assertTrue(r1fp.isBold());
        assertEquals(FontFormatting.SS_SUPER, r1fp.getEscapementType());
        assertEquals(FontFormatting.U_DOUBLE, r1fp.getUnderlineType());
        assertEquals(IndexedColors.BLUE.index, r1fp.getFontColorIndex());

    }

    public void testCreatePatternFormatting() {
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "7");
        PatternFormatting patternFmt = rule1.createPatternFormatting();

        assertEquals(0, patternFmt.getFillBackgroundColor());
        patternFmt.setFillBackgroundColor(IndexedColors.RED.index);
        assertEquals(IndexedColors.RED.index, patternFmt.getFillBackgroundColor());

        assertEquals(0, patternFmt.getFillForegroundColor());
        patternFmt.setFillForegroundColor(IndexedColors.BLUE.index);
        assertEquals(IndexedColors.BLUE.index, patternFmt.getFillForegroundColor());

        assertEquals(PatternFormatting.NO_FILL, patternFmt.getFillPattern());
        patternFmt.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        assertEquals(PatternFormatting.SOLID_FOREGROUND, patternFmt.getFillPattern());
        patternFmt.setFillPattern(PatternFormatting.NO_FILL);
        assertEquals(PatternFormatting.NO_FILL, patternFmt.getFillPattern());
        patternFmt.setFillPattern(PatternFormatting.BRICKS);
        assertEquals(PatternFormatting.BRICKS, patternFmt.getFillPattern());

        ConditionalFormattingRule [] cfRules = { rule1 };

        CellRangeAddress [] regions = { CellRangeAddress.valueOf("A1:A5") };

        sheetCF.addConditionalFormatting(regions, cfRules);

        // Verification
        ConditionalFormatting cf = sheetCF.getConditionalFormattingAt(0);
        assertNotNull(cf);

        assertEquals(1, cf.getNumberOfRules());

        PatternFormatting  r1fp = cf.getRule(0).getPatternFormatting();
        assertNotNull(r1fp);

        assertEquals(IndexedColors.RED.index, r1fp.getFillBackgroundColor());
        assertEquals(IndexedColors.BLUE.index, r1fp.getFillForegroundColor());
        assertEquals(PatternFormatting.BRICKS, r1fp.getFillPattern());
    }

    public void testCreateBorderFormatting() {
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "7");
        BorderFormatting borderFmt = rule1.createBorderFormatting();

        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderBottom());
        borderFmt.setBorderBottom(BorderFormatting.BORDER_DOTTED);
        assertEquals(BorderFormatting.BORDER_DOTTED, borderFmt.getBorderBottom());
        borderFmt.setBorderBottom(BorderFormatting.BORDER_NONE);
        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderBottom());
        borderFmt.setBorderBottom(BorderFormatting.BORDER_THICK);
        assertEquals(BorderFormatting.BORDER_THICK, borderFmt.getBorderBottom());

        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderTop());
        borderFmt.setBorderTop(BorderFormatting.BORDER_DOTTED);
        assertEquals(BorderFormatting.BORDER_DOTTED, borderFmt.getBorderTop());
        borderFmt.setBorderTop(BorderFormatting.BORDER_NONE);
        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderTop());
        borderFmt.setBorderTop(BorderFormatting.BORDER_THICK);
        assertEquals(BorderFormatting.BORDER_THICK, borderFmt.getBorderTop());

        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderLeft());
        borderFmt.setBorderLeft(BorderFormatting.BORDER_DOTTED);
        assertEquals(BorderFormatting.BORDER_DOTTED, borderFmt.getBorderLeft());
        borderFmt.setBorderLeft(BorderFormatting.BORDER_NONE);
        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderLeft());
        borderFmt.setBorderLeft(BorderFormatting.BORDER_THIN);
        assertEquals(BorderFormatting.BORDER_THIN, borderFmt.getBorderLeft());

        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderRight());
        borderFmt.setBorderRight(BorderFormatting.BORDER_DOTTED);
        assertEquals(BorderFormatting.BORDER_DOTTED, borderFmt.getBorderRight());
        borderFmt.setBorderRight(BorderFormatting.BORDER_NONE);
        assertEquals(BorderFormatting.BORDER_NONE, borderFmt.getBorderRight());
        borderFmt.setBorderRight(BorderFormatting.BORDER_HAIR);
        assertEquals(BorderFormatting.BORDER_HAIR, borderFmt.getBorderRight());

        ConditionalFormattingRule [] cfRules = { rule1 };

        CellRangeAddress [] regions = { CellRangeAddress.valueOf("A1:A5") };

        sheetCF.addConditionalFormatting(regions, cfRules);

        // Verification
        ConditionalFormatting cf = sheetCF.getConditionalFormattingAt(0);
        assertNotNull(cf);

        assertEquals(1, cf.getNumberOfRules());

        BorderFormatting  r1fp = cf.getRule(0).getBorderFormatting();
        assertNotNull(r1fp);
        assertEquals(BorderFormatting.BORDER_THICK, r1fp.getBorderBottom());
        assertEquals(BorderFormatting.BORDER_THICK, r1fp.getBorderTop());
        assertEquals(BorderFormatting.BORDER_THIN, r1fp.getBorderLeft());
        assertEquals(BorderFormatting.BORDER_HAIR, r1fp.getBorderRight());
    }
    
    // TODO Fix this test to work for HSSF
    public void DISABLEDtestCreateIconFormatting() {
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        ConditionalFormattingRule rule1 = 
                sheetCF.createConditionalFormattingRule(IconSet.GYRB_4_TRAFFIC_LIGHTS);
        IconMultiStateFormatting iconFmt = rule1.getMultiStateFormatting();
        
        assertEquals(IconSet.GYRB_4_TRAFFIC_LIGHTS, iconFmt.getIconSet());
        assertEquals(4, iconFmt.getThresholds().length);
        assertEquals(false, iconFmt.isIconOnly());
        assertEquals(false, iconFmt.isReversed());
        
        iconFmt.setIconOnly(true);
        iconFmt.getThresholds()[0].setRangeType(RangeType.MIN);
        iconFmt.getThresholds()[1].setRangeType(RangeType.NUMBER);
        iconFmt.getThresholds()[1].setValue(10d);
        iconFmt.getThresholds()[2].setRangeType(RangeType.PERCENT);
        iconFmt.getThresholds()[2].setValue(75d);
        iconFmt.getThresholds()[3].setRangeType(RangeType.MAX);
        
        CellRangeAddress [] regions = { CellRangeAddress.valueOf("A1:A5") };
        sheetCF.addConditionalFormatting(regions, rule1);
        
        // Save, re-load and re-check
        workbook = _testDataProvider.writeOutAndReadBack(workbook);
        sheetCF = sheet.getSheetConditionalFormatting();
        assertEquals(1, sheetCF.getNumConditionalFormattings());
        
        ConditionalFormatting cf = sheetCF.getConditionalFormattingAt(0);
        assertEquals(1, cf.getNumberOfRules());
        rule1 = cf.getRule(0);
        iconFmt = rule1.getMultiStateFormatting();
        
        assertEquals(IconSet.GYRB_4_TRAFFIC_LIGHTS, iconFmt.getIconSet());
        assertEquals(4, iconFmt.getThresholds().length);
        assertEquals(true, iconFmt.isIconOnly());
        assertEquals(false, iconFmt.isReversed());

        assertEquals(RangeType.MIN,    iconFmt.getThresholds()[0].getRangeType());
        assertEquals(RangeType.NUMBER, iconFmt.getThresholds()[1].getRangeType());
        assertEquals(RangeType.PERCENT,iconFmt.getThresholds()[2].getRangeType());
        assertEquals(RangeType.MAX,    iconFmt.getThresholds()[3].getRangeType());
        assertEquals(null, iconFmt.getThresholds()[0].getValue());
        assertEquals(10d,  iconFmt.getThresholds()[1].getValue());
        assertEquals(75d,  iconFmt.getThresholds()[2].getValue());
        assertEquals(null, iconFmt.getThresholds()[3].getValue());
    }
    
    public void testBug55380() {
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sheet = wb.createSheet();
        CellRangeAddress[] ranges = new CellRangeAddress[] {
            CellRangeAddress.valueOf("C9:D30"), CellRangeAddress.valueOf("C7:C31")
        };
        ConditionalFormattingRule rule = sheet.getSheetConditionalFormatting().createConditionalFormattingRule("$A$1>0");
        sheet.getSheetConditionalFormatting().addConditionalFormatting(ranges, rule);        
    }
}
