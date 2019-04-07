package org.aion4j.avm.helper.util;

import avm.Address;
import org.aion.avm.core.util.Helpers;
import org.aion4j.avm.helper.exception.MethodArgsParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class MethodCallArgsUtil {

    public static Object[] parseMethodArgs(String argsString) throws Exception {
        if(argsString == null || argsString.isEmpty())
            return new Object[0];

        String[] tokens = translateCommandline(argsString);

        List<Object> args = new ArrayList<>();

        boolean isType = false;
        String type = null;
        String prevType = null;

        List<Object> tempHolder = null;

        for(String token: tokens) {

            isType = isType(token);

            if(isType) {
                type = token;

                //add previous args.. before moving to next type
                if(tempHolder == null || tempHolder.size() == 0) {
                    tempHolder = new ArrayList<>();

                    prevType = type; //keep the previous type ..required for conversion
                    continue; //Seems like first one
                }

                if(tempHolder.size() == 1 && !isArrayType(prevType)) args.add(tempHolder.get(0)); //Only one value. No array.
                else {

                    if(is2DArrayType(prevType)) {
                        args.add(get2DArray(prevType, tempHolder));
                    } else { //One dimensional array
                        // args.add((String [])tempHolder.toArray(new String[0]));
                        args.add(getArray(prevType, tempHolder));
                    }
                }

                tempHolder.clear();

                prevType = type; //keep the previous type... required for conversion later.

            } else {

                if(is2DArrayType(type)) {
                    tempHolder.add(token); // don't convert now for 2DArray. It will be done later.
                } else {
                    Object value = convertStringToTypeObject(type, token);
                    tempHolder.add(value);
                }
            }
        }

        //add remaining
        if(tempHolder != null) {
            if (tempHolder.size() == 1 && !isArrayType(prevType)) args.add(tempHolder.get(0)); //Only one value. No array.
            else {
                if(is2DArrayType(prevType)) {
                    args.add(get2DArray(prevType, tempHolder));
                } else {
                    args.add(getArray(prevType, tempHolder)); //add the last item
                }
            }
        }

        return args.toArray();
    }

    private static boolean isArrayType(String type) {
        if(type == null) return false;

        if(type.endsWith("[]"))
            return true;
        else
            return false;
    }

    private static boolean is2DArrayType(String type) {
        if(type == null) return false;

        if(type.endsWith("[][]"))
            return true;
        else
            return false;
    }

    private static Object convertStringToTypeObject(String type, String token) throws MethodArgsParseException{
        if(type != null) {
            if(type.startsWith("-I")) return Integer.parseInt(token);
            else if(type.startsWith("-J")) return Long.valueOf(token);
            else if(type.startsWith("-S")) return Short.valueOf(token);
            else if(type.startsWith("-C")) return Character.valueOf(token.charAt(0));
            else if(type.startsWith("-F")) return Float.valueOf(token);
            else if(type.startsWith("-D")) return Double.valueOf(token);
            else if(type.startsWith("-B")) return Byte.valueOf(token);
            else if(type.startsWith("-Z")) return Boolean.valueOf(token);
            else if(type.startsWith("-A")) return new avm.Address(Helpers.hexStringToBytes(token));
            else if(type.startsWith("-T")) return token;
            else
                throw new MethodArgsParseException("Invalid type : " + type);
        } else {
            return null;
        }
    }

    private static Object getArray(String type, List list) {
        if(type.startsWith("-I")) {
            return toIntArray(list);
        }
        else if(type.startsWith("-J")) {
            return toLongArray(list);
        }
        else if(type.startsWith("-S")) {
            return toShortArray(list);
        }
        else if(type.startsWith("-C")) {
            return toCharArray(list);
        }
        else if(type.startsWith("-F")) {
            return toFloatArray(list);
        }
        else if(type.startsWith("-D")) {
            return toDoubleArray(list);
        }
        else if(type.startsWith("-B")) {
            return toByteArray(list);
        }
        else if(type.startsWith("-Z")) {
            return toBooleanArray(list);
        }
        else if(type.startsWith("-A")) {
            Address[] objs = new Address[list.size()];
            for(int i=0; i<list.size();i++) {
                objs[i] = (Address)list.get(i);
            }
           return objs;
        }
        else if(type.startsWith("-T")) {
            String[] objs = new String[list.size()];
            for(int i=0; i<list.size();i++) {
                objs[i] = (String)list.get(i);
            }
           return objs;
        }

        return null;
    }

    private static Object get2DArray(String type, List list) throws MethodArgsParseException {

        int rows = list.size();
        int cols = parseRowElementsIn2DArray((String)list.get(0)).length;

        //create  2D array
        Object a2dObj = init2DArray(type, rows, cols);

        for(int i=0; i<list.size(); i++) {

            String rawString = (String)list.get(i);
            String[] tokens = parseRowElementsIn2DArray(rawString);

            List elmList = new ArrayList();

            for(int j=0; j<tokens.length; j++) {
                Object value = convertStringToTypeObject(type, tokens[j]);
                assignValueIn2DArray(type, a2dObj, i, j, value);
            }
        }

        return a2dObj;
    }

    private static String[] parseRowElementsIn2DArray(String rowStr) throws MethodArgsParseException {

        if(rowStr == null)
            return new String[0];

        String[] rowElms = null;
        if(rowStr.contains(",")) {
            rowElms = rowStr.split("\\s*,\\s*");
        } else {
            rowElms = translateCommandline(rowStr);
        }

        for(int i=0; i<rowElms.length; i++) {
            rowElms[i] = rowElms[i] != null? rowElms[i].trim(): null;
        }

        return rowElms;
    }

    private static Object init2DArray(String type, int rows, int cols) {

        if (type.startsWith("-I[][]")) { ;
           return new int[rows][cols];
        } else if (type.startsWith("-J[][]")) {
            return new long[rows][cols];
        } else if (type.startsWith("-S[][]")) {
            return new short[rows][cols];
        } else if (type.startsWith("-C[][]")) {
            return new char[rows][cols];
        } else if (type.startsWith("-F[][]")) {
            return new float[rows][cols];
        } else if (type.startsWith("-D[][]")) {
            return new double[rows][cols];
        } else if (type.startsWith("-B[][]")) {
            return new byte[rows][cols];
        } else if (type.startsWith("-Z[][]")) {
            return new boolean[rows][cols];
        } else if (type.startsWith("-A[][]")) {
            return new Address[rows][cols];
        } else if (type.startsWith("-T[][]")) {
            return new String[rows][cols];
        }

        return null;
    }

    private static void assignValueIn2DArray(String type, Object arr, int row, int col, Object value) {
        if (type.startsWith("-I[][]")) { ;
            ((int[][])arr)[row][col] = (int)value;
        } else if (type.startsWith("-J[][]")) {
            ((long[][])arr)[row][col] = (long)value;
        } else if (type.startsWith("-S[][]")) {
            ((short[][])arr)[row][col] = (short)value;
        } else if (type.startsWith("-C[][]")) {
            ((char[][])arr)[row][col] = (char)value;
        } else if (type.startsWith("-F[][]")) {
            ((float[][])arr)[row][col] = (float)value;
        } else if (type.startsWith("-D[][]")) {
            ((double[][])arr)[row][col] = (double)value;
        } else if (type.startsWith("-B[][]")) {
            ((byte[][])arr)[row][col] = (byte)value;
        } else if (type.startsWith("-Z[][]")) {
            ((boolean[][])arr)[row][col] = (boolean)value;
        } else if (type.startsWith("-A[][]")) {
            ((Address[][])arr)[row][col] = (Address)value;
        } else if (type.startsWith("-T[][]")) {
            ((String[][])arr)[row][col] = (String)value;
        }
    }

    /**
     * Taken from maven-shared-utils package
     * @param toProcess The command line to translate.
     * @return The array of translated parts.
     * @throws MethodArgsParseException in case of unbalanced quotes.
     */
    public static String[] translateCommandline( String toProcess ) throws MethodArgsParseException
    {
        if ( ( toProcess == null ) || ( toProcess.length() == 0 ) )
        {
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        boolean inEscape = false;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer( toProcess, "\"\' \\", true );
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        while ( tok.hasMoreTokens() )
        {
            String nextTok = tok.nextToken();
            switch ( state )
            {
                case inQuote:
                    if ( "\'".equals( nextTok ) )
                    {
                        if ( inEscape )
                        {
                            current.append( nextTok );
                            inEscape = false;
                        }
                        else
                        {
                            state = normal;
                        }
                    }
                    else
                    {
                        current.append( nextTok );
                        inEscape = "\\".equals( nextTok );
                    }
                    break;
                case inDoubleQuote:
                    if ( "\"".equals( nextTok ) )
                    {
                        if ( inEscape )
                        {
                            current.append( nextTok );
                            inEscape = false;
                        }
                        else
                        {
                            state = normal;
                        }
                    }
                    else
                    {
                        current.append( nextTok );
                        inEscape = "\\".equals( nextTok );
                    }
                    break;
                default:
                    if ( "\'".equals( nextTok ) )
                    {
                        if ( inEscape )
                        {
                            inEscape = false;
                            current.append( nextTok );
                        }
                        else
                        {
                            state = inQuote;
                        }
                    }
                    else if ( "\"".equals( nextTok ) )
                    {
                        if ( inEscape )
                        {
                            inEscape = false;
                            current.append( nextTok );
                        }
                        else
                        {
                            state = inDoubleQuote;
                        }
                    }
                    else if ( " ".equals( nextTok ) )
                    {
                        if ( current.length() != 0 )
                        {
                            tokens.add( current.toString() );
                            current.setLength( 0 );
                        }
                    }
                    else
                    {
                        current.append( nextTok );
                        inEscape = "\\".equals( nextTok );
                    }
                    break;
            }
        }

        if ( current.length() != 0 )
        {
            tokens.add( current.toString() );
        }

        if ( ( state == inQuote ) || ( state == inDoubleQuote ) )
        {
            throw new MethodArgsParseException( "unbalanced quotes in " + toProcess );
        }

        return tokens.toArray( new String[tokens.size()] );
    }

    private static boolean isType(String token) {
        if(token == null) return false;

        switch (token) {
            case "-I":
            case "-J":
            case "-S":
            case "-C":
            case "-F":
            case "-D":
            case "-B":
            case "-Z":
            case "-A":
            case "-T":
            case "-I[]":
            case "-J[]":
            case "-S[]":
            case "-C[]":
            case "-F[]":
            case "-D[]":
            case "-B[]":
            case "-Z[]":
            case "-A[]":
            case "-T[]":
            case "-I[][]":
            case "-J[][]":
            case "-S[][]":
            case "-C[][]":
            case "-F[][]":
            case "-D[][]":
            case "-B[][]":
            case "-Z[][]":
            case "-A[][]":
            case "-T[][]":
                return true;
            default:
                return false;
        }
    }

    private static int[] toIntArray(List<Integer> list)  {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e;
        return ret;
    }

    private static long[] toLongArray(List<Long> list)  {
        long[] ret = new long[list.size()];
        int i = 0;
        for (Long e : list)
            ret[i++] = e;
        return ret;
    }

    private static short[] toShortArray(List<Short> list)  {
        short[] ret = new short[list.size()];
        int i = 0;
        for (Short e : list)
            ret[i++] = e;
        return ret;
    }

    private static char[] toCharArray(List<Character> list)  {
        char[] ret = new char[list.size()];
        int i = 0;
        for (Character e : list)
            ret[i++] = e;
        return ret;
    }

    private static float[] toFloatArray(List<Float> list)  {
        float[] ret = new float[list.size()];
        int i = 0;
        for (Float e : list)
            ret[i++] = e;
        return ret;
    }

    private static double[] toDoubleArray(List<Double> list)  {
        double[] ret = new double[list.size()];
        int i = 0;
        for (Double e : list)
            ret[i++] = e;
        return ret;
    }

    private static byte[] toByteArray(List<Byte> list)  {
        byte[] ret = new byte[list.size()];
        int i = 0;
        for (Byte e : list)
            ret[i++] = e;
        return ret;
    }

    private static boolean[] toBooleanArray(List<Boolean> list)  {
        boolean[] ret = new boolean[list.size()];
        int i = 0;
        for (Boolean e : list)
            ret[i++] = e;
        return ret;
    }

    // Multi dimensional return value in method call - helper methods
    /**
     * Used to print return value in a call method
     * @param data
     * @return
     */
    public static String print2DArray(Object data) {
        Class clazz = data.getClass();

        if (clazz == byte[][].class) {
            byte[][] arr = (byte[][])data;
            return print2Darray(arr);
        } else if (clazz == boolean[][].class) {
            boolean[][] arr = (boolean[][])data;
            return print2Darray(arr);
        } else if (clazz == char[][].class) {
            char[][] arr = (char[][])data;
            return print2Darray(arr);
        } else if (clazz == short[][].class) {
            short[][] arr = (short[][])data;
            return print2Darray(arr);
        } else if (clazz == int[][].class) {
            int[][] arr = (int[][])data;
            return print2Darray(arr);
        } else if (clazz == long[][].class) {
            long[][] arr = (long[][])data;
            return print2Darray(arr);
        } else if (clazz == float[][].class) {
            float[][] arr = (float[][]) data;
            return print2Darray(arr);
        } else if (clazz == double[][].class) {
            double[][] arr = (double[][])data;
            return print2Darray(arr);
        }

        return null;
    }

    public static String printArray(Object data) {
        Class clazz = data.getClass();

        if (clazz == byte[].class) {
            byte[] arr = (byte[])data;
            return Arrays.toString(arr);
        } else if (clazz == boolean[].class) {
            boolean[] arr = (boolean[])data;
            return Arrays.toString(arr);
        } else if (clazz == char[].class) {
            char[] arr = (char[])data;
            return Arrays.toString(arr);
        } else if (clazz == short[].class) {
            short[] arr = (short[])data;
            return Arrays.toString(arr);
        } else if (clazz == int[].class) {
            int[] arr = (int[])data;
            return Arrays.toString(arr);
        } else if (clazz == long[].class) {
            long[] arr = (long[])data;
            return Arrays.toString(arr);
        } else if (clazz == float[].class) {
            float[] arr = (float[]) data;
            return Arrays.toString(arr);
        } else if (clazz == double[].class) {
            double[] arr = (double[])data;
            return Arrays.toString(arr);
        } else if (clazz == Address[].class) {
            Address[] arr = (Address[])data;
            return Arrays.toString(arr);
        }else if (clazz == String[].class) {
            String[] arr = (String[])data;
            return Arrays.toString(arr);
        }

        return null;
    }

    private static String print2Darray(byte[][] objs) {
        StringBuilder sb = new StringBuilder();

        for(byte[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(boolean[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(boolean[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(short[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(short[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(int[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(int[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(long[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(long[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(char[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(char[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(float[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(float[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String print2Darray(double[][] objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\n");

        for(double[] arr: objs) {
            sb.append(Arrays.toString(arr));
            sb.append("\n");
        }

        return sb.toString();
    }
    //Method return decode value print ends

    public static void main(String[] args) throws Exception {

        String argsStr = "-A 0x1122334455667788112233445566778811223344556677881122334455667788 -J 100 -I 45 -B -1 -T hello";

        Object[] objs = parseMethodArgs(argsStr);

        for(Object obj: objs) {
            System.out.println(obj);
            System.out.println(obj.getClass());
        }
    }
}
