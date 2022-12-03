package Util;

import Model.DVR;
import Model.NodeServer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DisplayTable {
    List<String[]> rows = new LinkedList<>();

    public void addRow(String... cols){
        rows.add(cols);
    }


    String[] columnNames = {
            "Destination Server ID",
            "Next Hop Server ID",
            "Cost of Path"};




    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        int[] colWidths = colWidths();

        int i = 0, rowsSize = rows.size();
        while (i < rowsSize) {
            String[] row = rows.get(i);
            int colNum = 0;
            while (colNum < row.length) {
                sb.append(
                        StringUtils.rightPad(StringUtils.defaultString(
                                row[colNum]), colWidths[colNum])).append(' ');
                colNum++;
            }

            sb.append('\n');
            i++;
        }

        return sb.toString();
    }

    private int[] colWidths() {
        int cols = -1;

        for (Iterator<String[]> iterator = rows.iterator(); iterator.hasNext(); ) {
            String[] row = iterator.next();
            cols = Math.max(cols, row.length);
        }

        int[] widths = new int[cols];

        for (Iterator<String[]> iterator = rows.iterator(); iterator.hasNext(); ) {
            String[] row = iterator.next();
            int colNum = 0;
            while (colNum < row.length) {
                widths[colNum] =
                        Math.max(
                                widths[colNum],
                                StringUtils.length(row[colNum]));
                colNum++;
            }
        }

        return widths;
    }

    /**
     *
     * StringUtils.defaultString(null)  = ""
     * StringUtils.defaultString("")    = ""
     * StringUtils.defaultString("bat") = "bat"
     *
     *
     * StringUtils.rightPad(null, *)   = null
     *  StringUtils.rightPad("", 3)     = "   "
     *  StringUtils.rightPad("bat", 3)  = "bat"
     *  StringUtils.rightPad("bat", 5)  = "bat  "
     *  StringUtils.rightPad("bat", 1)  = "bat"
     *  StringUtils.rightPad("bat", -1) = "bat"
     *
     */




}
