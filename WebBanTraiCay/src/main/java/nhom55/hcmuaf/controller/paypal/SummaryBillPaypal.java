package nhom55.hcmuaf.controller.paypal;

import com.paypal.api.payments.PayerInfo;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.ShippingAddress;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nhom55.hcmuaf.beans.Users;
import nhom55.hcmuaf.beans.cart.Cart;
import nhom55.hcmuaf.beans.cart.CartProduct;
import nhom55.hcmuaf.dao.BillDao;
import nhom55.hcmuaf.dao.daoimpl.BillDaoImpl;
import nhom55.hcmuaf.sendmail.MailProperties;
import nhom55.hcmuaf.util.MyUtils;

@WebServlet(name = "SummaryBill", value = "/paypal/summary-bill-paypal")
public class SummaryBillPaypal extends HttpServlet {

  private static final long serialVersionUID = 1L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String paymentId = request.getParameter("paymentId");
    String payerId = request.getParameter("PayerID");

    try {
      PaymentServices paymentServices = new PaymentServices();
      Payment payment = paymentServices.getPaymentDetails(paymentId);
      PayerInfo payerInfo = payment.getPayer().getPayerInfo();
      Transaction transaction = payment.getTransactions().get(0);
      ShippingAddress shippingAddress = transaction.getItemList().getShippingAddress();
      //            request.setAttribute("payer", payerInfo);
//            request.setAttribute("transaction", transaction);
//            request.setAttribute("shippingAddress", shippingAddress);
      HttpSession session = request.getSession();
      Users users = MyUtils.getLoginedUser(session);

      String lastName = (String) session.getAttribute("lastName");
      String firstName = (String) session.getAttribute("firstName");
      String address = (String) session.getAttribute("address");
      String city = (String) session.getAttribute("city");
      String phoneNumber = (String) session.getAttribute("phoneNumber");
      String email = (String) session.getAttribute("email");
      String subtotal = (String) session.getAttribute("subtotal");
      double deliveryFee = (Double) session.getAttribute("deliveryFee");

      String note = (String) session.getAttribute("note");
      double subTotalPrice = Double.valueOf(subtotal);
      LocalDateTime timeNow = LocalDateTime.now();
      List<String> selectedProductIds = (List<String>) session.getAttribute("selectedProductIds");
      Cart cart = new Cart();
      List<CartProduct> selectedProducts = new ArrayList<>();
      if (cart != null && selectedProductIds != null) {
        // get product list selected from cart
        selectedProducts = cart.getSelectedProducts(selectedProductIds);
      }
      String productNameList = "";
      for (CartProduct c : selectedProducts) {
        productNameList += c.getProducts().getNameOfProduct() + "\t";
      }
      BillDao billDao = new BillDaoImpl();

      if (billDao.addAListProductToBills(timeNow, productNameList, "Đang giao", users.getId(), 2,
          firstName, lastName, address, city, phoneNumber, email, subTotalPrice, deliveryFee,
          note)) {

        int id_bills = billDao.getIDAListProductFromBills(timeNow, users.getId());
        for (CartProduct c : selectedProducts) {
          if (billDao.addAProductToBillDetails(c.getProducts().getId(), id_bills, c.getQuantity(),
              c.getQuantity() * c.getProducts().getPrice())) {
            billDao.degreeAmountWhenOderingSuccessfully(c.getProducts().getId(), c.getQuantity());
          }
        }
        // xoa san pham sau khi dat hang
        deleteCart(session);

        //          Thông báo người mua đã đặt thành công
        Properties smtpProperties = MailProperties.getSMTPPro();
        Session session1 = Session.getInstance(smtpProperties, new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(MailProperties.getEmail(),
                MailProperties.getPassword());
          }
        });
        try {
          Message message = new MimeMessage(session1);
          message.addHeader("Content-type", "text/HTML; charset= UTF-8");
          message.setFrom(new InternetAddress(MailProperties.getEmail()));
          message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
          message.setSubject("DAT HANG");
          message.setText("Don dat hang cua ban thanh cong. Xem don hang ban vua moi dat tai day : "
              + "http://localhost:8080/page/bill/detail?idBills="
              + id_bills);
          Transport.send(message);
          boolean isOrderSuccessfully = true;
          RequestDispatcher dispatcher = request.getRequestDispatcher("/page/shop/shop-forward");
          request.setAttribute("isOrderSuccessfully", isOrderSuccessfully);
          dispatcher.forward(request, response);
        } catch (Exception e) {
          System.out.println("SendEmail File Error " + e);
        }
      }


    } catch (PayPalRESTException ex) {
      request.setAttribute("errorMessage", ex.getMessage());
      ex.printStackTrace();
      request.getRequestDispatcher("error.jsp").forward(request, response);
    }
  }

  public static void deleteCart(HttpSession session) {
    List<String> selectedProductIds = (List<String>) session.getAttribute("selectedProductIds");
    Cart cart = (Cart) session.getAttribute("cart");
    for (String idProduct : selectedProductIds) {
      int id = Integer.valueOf(idProduct);
      cart.deleteProduct(id);
    }
  }
}
