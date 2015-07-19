/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.ss.usermodel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.ITestDataProvider;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.SXSSFITestDataProvider;
import org.apache.poi.xssf.XSSFITestDataProvider;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCell;

/**
 * Class for combined testing of XML-specific functionality of 
 * {@link XSSFCell} and {@link SXSSFCell}.
 * 
 *  Any test that is applicable for {@link HSSFCell} as well should go into
 *  the common base class {@link BaseTestCell}.
 */
public abstract class BaseTestXCell extends BaseTestCell {
    protected BaseTestXCell(ITestDataProvider testDataProvider) {
        super(testDataProvider);
    }

    public void testXmlEncoding(){
        Workbook wb = _testDataProvider.createWorkbook();
        Sheet sh = wb.createSheet();
        Row row = sh.createRow(0);
        Cell cell = row.createCell(0);
        String sval = "\u0000\u0002\u0012<>\t\n\u00a0 &\"POI\'\u2122";
        cell.setCellValue(sval);

        wb = _testDataProvider.writeOutAndReadBack(wb);

        // invalid characters are replaced with question marks
        assertEquals("???<>\t\n\u00a0 &\"POI\'\u2122", wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
    }
}
