package yuu.xposed;

import android.util.Log;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.update.Update;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QueryParser {

    private String TAG = "QueryParser";

    private String query;

    public QueryParser(String query) {
        setQuery(query);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = cleanQuery(query);
    }

    private String cleanQuery(String query) {
        if (query != null) {
            // Remove the injection marker if present in the SQL
            if (query.contains("<injection>")) {
                query = query.replace("<injection>", "");
            }
            // No quotes around table names in CREATE statements
            if (query.trim().toLowerCase().startsWith("create ")) {
                String createSubstr = query.substring(0, query.indexOf('('));
                createSubstr = createSubstr.replace("'", "");
                createSubstr = createSubstr.replace("\"", "");
                query = createSubstr + query.substring(query.indexOf('('));
            }
            // Remove ON CONFLICT statements
            if (query.toLowerCase().contains("on conflict")) {
                Pattern p = Pattern.compile("on\\s+conflict\\s+(replace)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(query);
                query = m.replaceAll("");
            }
        }
        return query;
    }

    private void parseError(JSQLParserException jpe, String method) {
        Log.e(TAG, "Parser exception in method " + method + ": [" + jpe.getMessage() + "]. SQL: " + query);
    }

    private ArrayList<Column> getCols() {
        if (isCreate()) {
            return getCreateCols(query);
        } else if (isSelect()) {
            return getQueryCols(query);
        } else if (isUpdate()) {
            return getUpdateCols(query);
        }
        return null;
    }

    public boolean isCreate() {
        return query != null && query.trim().toLowerCase().startsWith("create ");
    }

    public boolean isSelect() {
        return query != null && query.trim().toLowerCase().startsWith("select ");
    }

    public boolean isUpdate() {
        return query != null && query.trim().toLowerCase().startsWith("update ");
    }

    public String getInjectionVectorAlias() {
        // If the injection vector placeholder () is not present return null
        if (query == null || !query.contains("<injection>")) {
            return null;
        }

        Pattern p = Pattern.compile("<injection>\\s+AS\\s+([\\w\\d_-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(query);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private ArrayList<Column> getQueryCols(String query) {
        ArrayList<Column> cols = new ArrayList<>();

        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(query);
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getQueryCols()");
            return cols;
        }

        for (SelectItem selectItem : ((PlainSelect) stmt.getSelectBody()).getSelectItems()) {
            selectItem.accept(new SelectItemVisitorAdapter() {
                @Override
                public void visit(SelectExpressionItem item) {
                    if (item.getAlias() != null) {
                        cols.add(new Column(item.getExpression().toString(), item.getAlias().getName()));
                    } else {
                        cols.add(new Column(item.getExpression().toString(), null));
                    }
                }
            });
        }

        return cols;
    }

    private ArrayList<Column> getUpdateCols(String query) {
        ArrayList<Column> cols = new ArrayList<>();

        Update stmt;
        try {
            stmt = (Update) CCJSqlParserUtil.parse(query);
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getUpdateCols()");
            return cols;
        }

        for (net.sf.jsqlparser.schema.Column updateCol : stmt.getUpdateSets().get(0).getColumns()) {
            cols.add(new Column(updateCol.getColumnName(), null));
        }

        return cols;
    }

    private ArrayList<Column> getCreateCols(String query) {
        ArrayList<Column> cols = new ArrayList<>();

        CreateTable stmt;
        try {
            stmt = (CreateTable) CCJSqlParserUtil.parse(query);
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getCreateCols()");
            return cols;
        }

        for (ColumnDefinition createCol : stmt.getColumnDefinitions()) {
            cols.add(new Column(createCol.getColumnName().replace("`", ""), null));
        }

        return cols;
    }

    public boolean isWildcard() {
        Pattern matchWildcardPattern = Pattern.compile("SELECT\\s+\\*\\s+FROM\\s+([\\w-]+)\\s?", Pattern.CASE_INSENSITIVE);
        Matcher matchWildcardQuery = matchWildcardPattern.matcher(query);
        return matchWildcardQuery.find();
    }

    public ArrayList<String> getCols(String filter) {
        ArrayList<Column> cols = getCols();
        LinkedHashSet<String> strCols = new LinkedHashSet<>();
        if (cols != null) {
            for (Column col : cols) {
                if (filter == null || col.matchName(filter)) {
                    strCols.add(col.name);
                }
            }
        }
        return new ArrayList<>(strCols);
    }

    public ArrayList<String> getWrappedCols(String wrap) {
        ArrayList<Column> cols = getCols();
        LinkedHashSet<String> strCols = new LinkedHashSet<>();
        if (cols != null) {
            for (Column col : cols) {
                strCols.add(String.format(wrap, col.name));
            }
        }
        return new ArrayList<>(strCols);
    }

    public String getFrom() {
        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = ((PlainSelect) stmt.getSelectBody());
            if (plainSelect.getFromItem() != null) {
                return plainSelect.getFromItem().toString();
            }
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getFrom()");
        }
        return null;
    }

    public String getOrderBy() {
        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = ((PlainSelect) stmt.getSelectBody());
            if (plainSelect.getOrderByElements() != null && plainSelect.getOrderByElements().size() > 0) {
                ArrayList<String> orderByStr = new ArrayList<>();
                for (OrderByElement orderBy : plainSelect.getOrderByElements()) {
                    orderByStr.add(orderBy.toString());
                }
                return String.join(", ", orderByStr);
            }
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getOrderBy()");
        }
        return null;
    }

    public String removeLimitOffset() {
        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = ((PlainSelect) stmt.getSelectBody());
            if (plainSelect.getLimit() != null) {
                plainSelect.setLimit(null);
                return plainSelect.toString();
            }
        } catch (JSQLParserException jpe) {
            parseError(jpe, "removeLimitOffset()");
        }
        return null;
    }

    public int[] getLimitOffset() {
        Select stmt;
        int limit = -1;
        int offset = -1;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(query);
            PlainSelect plainSelect = ((PlainSelect) stmt.getSelectBody());
            if (plainSelect.getLimit() != null) {
                limit = Integer.parseInt(plainSelect.getLimit().getRowCount().toString());
                if (plainSelect.getLimit().getOffset() != null) {
                    offset = Integer.parseInt(plainSelect.getLimit().getOffset().toString());
                }
                return new int[]{limit, offset};
            }
        } catch (JSQLParserException jpe) {
            parseError(jpe, "getLimitOffset()");
        }
        return null;
    }

    private class Column {
        private String name;
        private String alias;

        public Column(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public boolean matchName(String filter) {
            if (this.alias == null) {
                return this.name.contains(filter);
            } else {
                return this.name.contains(filter) || this.alias.contains(filter);
            }
        }
    }

}
