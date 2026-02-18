import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:library.db";
        try (Connection c = DriverManager.getConnection(url)) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet t = md.getTables(null, null, "product_barcodes", null)) {
                System.out.println("table_product_barcodes=" + t.next());
            }
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery("select count(*) from products")) {
                    if (rs.next()) System.out.println("products_count=" + rs.getInt(1));
                }
                try (ResultSet rs = s.executeQuery("select count(*) from product_barcodes")) {
                    if (rs.next()) System.out.println("product_barcodes_count=" + rs.getInt(1));
                } catch (SQLException ex) {
                    System.out.println("product_barcodes_count=ERR:" + ex.getMessage());
                }
                try (ResultSet rs = s.executeQuery("select p.product_id, p.product_name, p.barcode, pb.barcode as extra from products p left join product_barcodes pb on p.product_id=pb.product_id order by p.product_id desc limit 25")) {
                    int i=0;
                    while (rs.next()) {
                        i++;
                        System.out.println("row"+i+"="+rs.getLong("product_id")+"|"+rs.getString("product_name")+"|main="+rs.getString("barcode")+"|extra="+rs.getString("extra"));
                    }
                    if (i==0) System.out.println("rows=0");
                } catch (SQLException ex) {
                    System.out.println("sample_rows=ERR:" + ex.getMessage());
                }
            }
        }
    }
}
