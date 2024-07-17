package nhom55.hcmuaf.controller.paypal;

import com.paypal.base.rest.PayPalRESTException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import nhom55.hcmuaf.beans.Bills;
import nhom55.hcmuaf.beans.Users;
import nhom55.hcmuaf.beans.cart.Cart;
import nhom55.hcmuaf.beans.cart.CartProduct;
import nhom55.hcmuaf.util.MyUtils;

@WebServlet(name = "AuthorizePayPal", value = "/paypal/authorize-payment")
public class AuthorizePaymentServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  public AuthorizePaymentServlet() {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HttpSession session = request.getSession();
    Users users = MyUtils.getLoginedUser(session);
    List<String> selectedProductIds = (List<String>) session.getAttribute("selectedProductIds");
    Cart cart = new Cart();
    List<CartProduct> selectedProducts = new ArrayList<>();
    if (cart != null && selectedProductIds != null) {
      // get product list selected from cart
      selectedProducts = cart.getSelectedProducts(selectedProductIds);
    }
    String productName = "";
    //        Tổng tiền trái cây mà User định thanh toán
    double subtotal = 0;
    for (CartProduct cp : selectedProducts) {
      subtotal += cp.getProducts().getPrice();
      productName += cp.getProducts().getNameOfProduct() + ",";
    }
    String lastName = request.getParameter("ho_nguoi-dung");
    String firstName = request.getParameter("ten_nguoi-dung");
    String address = request.getParameter("dia-chi_nguoi-dung");
    String city = request.getParameter("provinceName");
    String district = request.getParameter("districtName");
    String phoneNumber = request.getParameter("sdt_nguoi-dung");
    String email = request.getParameter("email_nguoi-dung");
    String deliveryFee = request.getParameter("delivery_fee");
    String cleanedString = deliveryFee.replaceAll("[₫\\s]", "");
    cleanedString = cleanedString.replace(".", "");
    double deliveryFeeDouble = Double.parseDouble(cleanedString);

    String note = request.getParameter("note_nguoi-dung");
    //       Mặc định cho thanh toán Paypal có idPayment = 2
    int idPayment = 2;
    address += address + ", quận " + district + ", tỉnh " + city;

    Bills bills = new Bills();
    bills.setProductList(productName);
    bills.setStatus("Đang giao");
    bills.setUserId(users.getId());
    bills.setPayment(idPayment);
    bills.setFirstName(firstName);
    bills.setLastName(lastName);
    bills.setStreetAddress(address);
    bills.setCity(city);
    bills.setPhoneNumber(phoneNumber);
    bills.setEmail(email);
    bills.setTotalPrice(subtotal);
    bills.setDeliveryFee(deliveryFeeDouble);

    bills.setNote(note);
    try {
      PaymentServices paymentServices = new PaymentServices();
      String approvalLink = paymentServices.authorizePayment(bills);
      session.setAttribute("lastName", lastName);
      session.setAttribute("firstName", firstName);
      session.setAttribute("address", address);
      session.setAttribute("city", city);
      session.setAttribute("phoneNumber", phoneNumber);
      session.setAttribute("email", email);
      session.setAttribute("subtotal", subtotal);
      session.setAttribute("deliveryFee", deliveryFeeDouble);

      session.setAttribute("note", note);

      response.sendRedirect(approvalLink);


    } catch (PayPalRESTException ex) {
      request.setAttribute("errorMessage", ex.getMessage());
      ex.printStackTrace();
//            request.getRequestDispatcher("error.jsp").forward(request, response);
    }
  }
}