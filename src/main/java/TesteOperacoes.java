/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Calendar;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author deivid
 */
public class TesteOperacoes {

    //senha do certificado (Certificate Password)
    public static String senhaCertificado = "1234";
    //arquivo jks 
    public static String arquivoJks = "arquivo.jks";
    //usuario (User ID)
    public static String usuario = "1234";
    //senha do usuario  (User Password)
    public static String senhaUsuario = "1234";
    //ambiente da api se 2 = teste se 1=producao
    public static String ambiente = "2";
    //pasta onde os xmls de requiscao e respostas serao salvos
    public static File pasta;

    static {
        //cria a pasta senao existir
        pasta = new File(System.getProperty("user.dir") + File.separator + "vendas");
        if (!pasta.exists()) {
            pasta.mkdirs();
        }
    }

    public static void main(String[] args) {
        try {
            //venda cartao de credito
            String pedido = "pedido-" + Calendar.getInstance().getTimeInMillis();//gera um pedido baseado na data hora
            vendaComCartaoDeCredito("5105---5617---9411---8996", "04", "26", "123", "30.05", pedido, false, "", "");
            //
            //venda cartao de credito parcelado sem juros
            // vendaComCartaoDeCredito("5485---7392---3885---8589", "04", "26", "123", "200.00", "pedido-" + Calendar.getInstance().getTimeInMillis(), true, "3", "no");
            //
            //venda cartao de credito parcelado com juros juros
            // vendaComCartaoDeCredito("5148---6800---0000---0019", "04", "26", "123", "150.05", "pedido-" + Calendar.getInstance().getTimeInMillis(), true, "6", "yes");
            //
            //cancela pedido no mesmo dia
            //cancelarPedidoMesmoDia("pedido-1619199390111", "1619199393");
            //
            //consulta pedido 
            consultarPedido(pedido);
            //
            //cancela pedido no dia+1
            //cancelarPedidoDiaPosterior(pedido, "30.05");
            //
            //obter pedido dentro de um intervalo
            // consultarPedidosPorPeriodo("2021-04-05T10:23:37.143+02:00", "2021-05-05T10:23:37.143+02:00");
            //
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param pedido numero do pedido
     * @param total total do pedido
     * @throws ParseException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static void cancelarPedidoDiaPosterior(String pedido, String total) throws ParseException, SAXException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, IOException, ParserConfigurationException {
        System.out.println("#####################################################");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Header />\n"
                + "<SOAP-ENV:Body>\n"
                + "<ipgapi:IPGApiOrderRequest\n"
                + "xmlns:v1=\"http://ipg-online.com/ipgapi/schemas/v1\"\n"
                + "xmlns:ipgapi=\"http://ipg-online.com/ipgapi/schemas/ipgapi\">\n"
                + "<v1:Transaction>\n"
                + "<v1:CreditCardTxType>\n"
                + "<v1:Type>return</v1:Type>\n"
                + "</v1:CreditCardTxType>\n"
                + "<v1:Payment>\n"
                + "<v1:ChargeTotal>" + total + "</v1:ChargeTotal>\n"
                + "<v1:Currency>986</v1:Currency>\n"
                + "</v1:Payment>\n"
                + "<v1:TransactionDetails>\n"
                + "<v1:OrderId>" + pedido + "</v1:OrderId>\n"
                + "</v1:TransactionDetails>\n"
                + "</v1:Transaction>\n"
                + "</ipgapi:IPGApiOrderRequest>"
                + "</SOAP-ENV:Body>\n"
                + "</SOAP-ENV:Envelope>";
        // System.out.println(xml);
        FileUtils.writeStringToFile(
                new File(pasta + File.separator + "requisicao-cancelar-diaposterior-" + pedido + ".xml"),
                xml,
                "utf8");

        HttpResponse response = criarConexaoProcessarXml(xml);
        System.out.println("status:" + response.getStatusLine().getStatusCode());
        System.out.println("pedido:" + pedido);
        System.out.println("total:" + total);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                // System.out.println("xml response:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-cancelar-diaposterior-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //

                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //

                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);
                System.out.println("cancelado com sucesso");

            }

        } else {

            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                //  System.out.println("xml erro:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-cancelar-diaposterior-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String errormessage = getString("ipgapi:ErrorMessage", rootElement);
                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //
                System.out.println("erro:" + errormessage);
                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);
                System.out.println("erro ao cancelar");
            }
        }
    }

    /**
     *
     * @param pedido numero do pedido
     * @param tdate data hora da transacao
     * @throws ParseException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static void cancelarPedidoMesmoDia(String pedido, String tdate) throws ParseException, SAXException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, IOException, ParserConfigurationException {
        System.out.println("#####################################################");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Header />\n"
                + "<SOAP-ENV:Body>\n"
                + "<ipgapi:IPGApiOrderRequest\n"
                + "xmlns:v1=\"http://ipg-online.com/ipgapi/schemas/v1\"\n"
                + "xmlns:ipgapi=\"http://ipg-online.com/ipgapi/schemas/ipgapi\">\n"
                + "<v1:Transaction>\n"
                + "<v1:CreditCardTxType>\n"
                + "<v1:Type>void</v1:Type>\n"
                + "</v1:CreditCardTxType>\n"
                + "<v1:TransactionDetails>\n"
                + "<v1:OrderId>" + pedido + "</v1:OrderId>\n"
                + "<v1:TDate>" + tdate + "</v1:TDate>\n"
                + "</v1:TransactionDetails>\n"
                + "</v1:Transaction>\n"
                + "</ipgapi:IPGApiOrderRequest>"
                + "</SOAP-ENV:Body>\n"
                + "</SOAP-ENV:Envelope>";
        // System.out.println(xml);
        FileUtils.writeStringToFile(
                new File(pasta + File.separator + "requisicao-cancelar-mesmodia-" + pedido + ".xml"),
                xml,
                "utf8");

        HttpResponse response = criarConexaoProcessarXml(xml);
        System.out.println("status:" + response.getStatusLine().getStatusCode());
        System.out.println("pedido:" + pedido);
        System.out.println("tdate:" + tdate);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                // System.out.println("xml response:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-cancelar-mesmodia-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //

                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //

                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);
                System.out.println("cancelado com sucesso");

            }

        } else {

            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                //  System.out.println("xml erro:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-cancelar-mesmodia-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String errormessage = getString("ipgapi:ErrorMessage", rootElement);
                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //
                System.out.println("erro:" + errormessage);
                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);
                System.out.println("erro ao cancelar");

            }
        }
    }

    /**
     *
     * @param datainicio data hora inicio
     * @param datafim data hora fim
     * @throws ParseException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static void consultarPedidosPorPeriodo(String datainicio, String datafim) throws ParseException, SAXException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, IOException, ParserConfigurationException {
        System.out.println("#####################################################");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Header />\n"
                + "<SOAP-ENV:Body>\n"
                + "<ns5:IPGApiActionRequest xmlns:ns5=\"http://ipg-online.com/ipgapi/schemas/ipgapi\" xmlns:ns2=\"http://ipg-online.com/ipgapi/schemas/v1\" xmlns:ns3=\"http://ipg-online.com/ipgapi/schemas/a1\"\n"
                + " >\n"
                + "<ns3:Action>\n"
                + "<ns3:GetLastOrders>\n"
                + "<ns3:Count>30</ns3:Count>\n"
                + "<ns3:DateFrom>" + datainicio + "</ns3:DateFrom>\n"
                + "<ns3:DateTo>" + datafim + "</ns3:DateTo>\n"
                + "</ns3:GetLastOrders>\n"
                + "</ns3:Action>\n"
                + "</ns5:IPGApiActionRequest>\n"
                + "</SOAP-ENV:Body>\n"
                + "</SOAP-ENV:Envelope>";
        // System.out.println(xml);
        FileUtils.writeStringToFile(
                new File(pasta + File.separator + "requisicao-consulta-periodo" + datainicio.replaceAll("\\D", "") + "-" + datafim.replaceAll("\\D", "") + ".xml"),
                xml,
                "utf8");

        HttpResponse response = criarConexaoProcessarXml(xml);
        System.out.println("status:" + response.getStatusLine().getStatusCode());
        System.out.println("periodo:" + datainicio + " - " + datafim);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                // System.out.println("xml response:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-consulta-periodo" + datainicio.replaceAll("\\D", "") + "-" + datafim.replaceAll("\\D", "") + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //

                String sucesso = getString("ipgapi:successfully", rootElement);
                //
                System.out.println("sucesso:" + sucesso);

                if (sucesso.equals("true")) {
                    NodeList nl = rootElement.getElementsByTagName("ipgapi:OrderValues");
                    int length = nl.getLength();
                    for (int i = 0; i < length; i++) {
                        if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                            Element el = (Element) nl.item(i);
                            System.out.println("##################33detalhe pedido#####################");
                            String orderid = getString("ipgapi:OrderId", el);
                            String datahora = getString("a1:OrderDate", el);
                            String cartaonum = getString("v1:CardNumber", el);
                            String mesvalidade = getString("v1:ExpMonth", el);
                            String anovalidade = getString("v1:ExpYear", el);
                            String total = getString("v1:ChargeTotal", el);
                            String transactionid = getString("ipgapi:IpgTransactionId", el);
                            String bandeira = getString("ipgapi:Brand", el);
                            String tipopag = getString("ipgapi:PaymentType", el);
                            String code = getString("ipgapi:ProcessorApprovalCode", el);
                            String tracenumber = getString("a1:TraceNumber", el);
                            String tipotransacao = getString("a1:TransactionType", el);
                            String estadotransacao = getString("a1:TransactionState", el);
                            //

                            System.out.println("pedido:" + orderid);
                            System.out.println("cartao:" + cartaonum);
                            System.out.println("mes/ano validade:" + mesvalidade + "/" + anovalidade);
                            System.out.println("total:" + total);
                            System.out.println("transacao:" + transactionid);
                            System.out.println("bandeira:" + bandeira);
                            System.out.println("tipopag:" + tipopag);
                            System.out.println("codigo:" + code);
                            System.out.println("trace number:" + tracenumber);
                            System.out.println("tipo transacao:" + tipotransacao);
                            System.out.println("estado transacao:" + estadotransacao);

                        }
                    }

                }

            }

        } else {

            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                //  System.out.println("xml erro:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-consulta-periodo" + datainicio.replaceAll("\\D", "") + "-" + datafim.replaceAll("\\D", "") + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String errormessage = getString("ipgapi:ErrorMessage", rootElement);
                String sucesso = getString("ipgapi:successfully", rootElement);
                //
                System.out.println("erro:" + errormessage);
                System.out.println("sucesso:" + sucesso);

            }
        }
    }

    /**
     *
     * @param pedido numero do pedido
     * @throws ParseException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static void consultarPedido(String pedido) throws ParseException, SAXException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, IOException, ParserConfigurationException {
        System.out.println("#####################################################");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<SOAP-ENV:Header />\n"
                + "<SOAP-ENV:Body>\n"
                + "<ns4:IPGApiActionRequest\n"
                + "xmlns:ns4=\"http://ipg-online.com/ipgapi/schemas/ipgapi\"\n"
                + "xmlns:ns2=\"http://ipg-online.com/ipgapi/schemas/a1\"\n"
                + "xmlns:ns3=\"http://ipg-online.com/ipgapi/schemas/v1\">\n"
                + "<ns2:Action>\n"
                + "<ns2:InquiryOrder>\n"
                + "<ns2:OrderId>" + pedido + "</ns2:OrderId>\n"
                + "</ns2:InquiryOrder>\n"
                + "</ns2:Action>\n"
                + "</ns4:IPGApiActionRequest>\n"
                + "</SOAP-ENV:Body>\n"
                + "</SOAP-ENV:Envelope>";
        // System.out.println(xml);
        FileUtils.writeStringToFile(
                new File(pasta + File.separator + "requisicao-consulta-" + pedido + ".xml"),
                xml,
                "utf8");

        HttpResponse response = criarConexaoProcessarXml(xml);
        System.out.println("status:" + response.getStatusLine().getStatusCode());
        System.out.println("pedido:" + pedido);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                // System.out.println("xml response:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-consulta-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //

                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //

                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);

            }

        } else {

            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                //  System.out.println("xml erro:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-consulta-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String errormessage = getString("ipgapi:ErrorMessage", rootElement);
                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tracenumber = getString("a1:TraceNumber", rootElement);
                String tipotransacao = getString("a1:TransactionType", rootElement);
                String estadotransacao = getString("a1:TransactionState", rootElement);
                //
                System.out.println("erro:" + errormessage);
                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("codigo:" + code);
                System.out.println("trace number:" + tracenumber);
                System.out.println("tipo transacao:" + tipotransacao);
                System.out.println("estado transacao:" + estadotransacao);

            }
        }
    }

    /**
     *
     * @param numeroCartao numero do cartao de credito
     * @param mesValidade mes de validade do cartao
     * @param anoValidade ano de validade do cartao
     * @param cvc codigo de seguranca do cartao
     * @param total total do pedido
     * @param pedido numero do pedido
     * @param parcelar se true ira parcelar o pedido se false nao
     * @param parcelas quantidade de parcelas
     * @param aplicarJuros se yes aplica juros se no não aplica juros
     * @throws ParseException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws KeyStoreException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static void vendaComCartaoDeCredito(String numeroCartao,
            String mesValidade,
            String anoValidade,
            String cvc,
            String total,
            String pedido,
            boolean parcelar,
            String parcelas,
            String aplicarJuros
    ) throws ParseException, SAXException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, IOException, ParserConfigurationException {
        System.out.println("#####################################################");

        String parcelado = "";
        if (parcelar) {
            parcelado = "<v1:numberOfInstallments>" + parcelas + "</v1:numberOfInstallments>"
                    + "<v1:installmentsInterest>" + aplicarJuros + "</v1:installmentsInterest>"
                    + "<v1:installmentDelayMonths>1</v1:installmentDelayMonths>";
        }

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <SOAP-ENV:Header />\n"
                + "    <SOAP-ENV:Body>\n"
                + "        <ipgapi:IPGApiOrderRequest xmlns:v1=\"http://ipg-online.com/ipgapi/schemas/v1\" xmlns:ipgapi=\"http://ipg-online.com/ipgapi/schemas/ipgapi\">\n"
                + "            <v1:Transaction>\n"
                + "                <v1:CreditCardTxType>\n"
                + "                    <v1:Type>sale</v1:Type>\n"
                + "                </v1:CreditCardTxType>\n"
                + "                <v1:CreditCardData>\n"
                + "                    <v1:CardNumber>" + numeroCartao.replaceAll("\\D", "") + "</v1:CardNumber>\n"
                + "                    <v1:ExpMonth>" + mesValidade + "</v1:ExpMonth>\n"
                + "                    <v1:ExpYear>" + anoValidade + "</v1:ExpYear>"
                + "                    <v1:CardCodeValue>" + cvc + "</v1:CardCodeValue>\n"
                + "                </v1:CreditCardData>\n"
                + "                <v1:cardFunction>credit</v1:cardFunction>\n"
                + "                <v1:Payment>\n"
                + "                     " + parcelado + ""
                + "                    <v1:ChargeTotal>" + total + "</v1:ChargeTotal>\n"
                + "                    <v1:Currency>986</v1:Currency>\n"
                + "                </v1:Payment>"
                + "                <v1:TransactionDetails>\n"
                + "                     <v1:OrderId>" + pedido + "</v1:OrderId>\n"
                + "                </v1:TransactionDetails>\n"
                + "            </v1:Transaction>\n"
                + "        </ipgapi:IPGApiOrderRequest>\n"
                + "    </SOAP-ENV:Body>\n"
                + "</SOAP-ENV:Envelope>";
        // System.out.println(xml);
        FileUtils.writeStringToFile(
                new File(pasta + File.separator + "requisicao-" + pedido + ".xml"),
                xml,
                "utf8");

        HttpResponse response = criarConexaoProcessarXml(xml);
        System.out.println("status:" + response.getStatusLine().getStatusCode());
        System.out.println("cartao:" + numeroCartao);
        System.out.println("validade cartao:" + mesValidade + "/" + anoValidade);
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                // System.out.println("xml response:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String referencenumber = getString("ipgapi:ProcessorReferenceNumber", rootElement);
                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String result = getString("ipgapi:TransactionResult", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                String tdate = getString("ipgapi:TDate", rootElement);
                //
                System.out.println("reference number:" + referencenumber);
                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("resultado" + result);
                System.out.println("tdate:" + tdate);
                System.out.println("codigo:" + code);

            }

        } else {

            HttpEntity entityr = response.getEntity();
            try (InputStream is = entityr.getContent()) {
                String content = EntityUtils.toString(entityr);
                //  System.out.println("xml erro:" + content);
                FileUtils.writeStringToFile(
                        new File(pasta + File.separator + "resposta-" + pedido + ".xml"),
                        content,
                        "utf8");
                //
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(content)));
                Element rootElement = document.getDocumentElement();
                //
                String errormessage = getString("ipgapi:ErrorMessage", rootElement);
                String orderid = getString("ipgapi:OrderId", rootElement);
                String transactionid = getString("ipgapi:IpgTransactionId", rootElement);
                String bandeira = getString("ipgapi:Brand", rootElement);
                String tipopag = getString("ipgapi:PaymentType", rootElement);
                String result = getString("ipgapi:TransactionResult", rootElement);
                String code = getString("ipgapi:ProcessorApprovalCode", rootElement);
                //
                System.out.println("erro:" + errormessage);
                System.out.println("pedido:" + orderid);
                System.out.println("transacao:" + transactionid);
                System.out.println("bandeira:" + bandeira);
                System.out.println("tipopag:" + tipopag);
                System.out.println("resultado" + result);
                System.out.println("codigo:" + code);
            }
        }
    }

    /**
     *
     * @param xml string do xml da venda ou acao a ser feita
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     */
    private static HttpResponse criarConexaoProcessarXml(String xml) throws NoSuchAlgorithmException, UnsupportedEncodingException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, IOException, CertificateException {
        String keyPassphrase = senhaCertificado;
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(arquivoJks), keyPassphrase.toCharArray());
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyPassphrase.toCharArray())
                .build();
        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();
        HttpPost httppost = new HttpPost(ambiente.equals("2") ? "https://test.ipg-online.com/ipgapi/services" : "https://www2.ipg-online.com/ipgapi/services");
        String encoding = Base64.getEncoder().encodeToString((usuario + ":" + senhaUsuario).getBytes());
        httppost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        //
        StringEntity entity = new StringEntity(xml.trim());
        httppost.setEntity(entity);
        httppost.setHeader("Accept", "text/xml");
        httppost.setHeader("Content-type", "text/xml");
        //
        HttpResponse response = httpClient.execute(httppost);
        return response;
    }

    protected static String getString(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
            NodeList subList = list.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }

}
