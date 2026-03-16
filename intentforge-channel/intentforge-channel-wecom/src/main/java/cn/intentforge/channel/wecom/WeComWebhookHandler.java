package cn.intentforge.channel.wecom;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.ChannelAccountProfile;
import cn.intentforge.channel.ChannelInboundMessage;
import cn.intentforge.channel.ChannelParticipant;
import cn.intentforge.channel.ChannelTarget;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookHandler;
import cn.intentforge.channel.ChannelWebhookRequest;
import cn.intentforge.channel.ChannelWebhookResponse;
import cn.intentforge.channel.ChannelWebhookResult;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

/**
 * Normalizes WeCom callback payloads into shared channel inbound messages.
 *
 * @since 1.0.0
 */
final class WeComWebhookHandler implements ChannelWebhookHandler {
  private static final ChannelWebhookResponse SUCCESS_RESPONSE =
      new ChannelWebhookResponse(200, "text/plain; charset=utf-8", "success", Map.of());

  private final ChannelAccountProfile accountProfile;

  WeComWebhookHandler(ChannelAccountProfile accountProfile) {
    this.accountProfile = Objects.requireNonNull(accountProfile, "accountProfile must not be null");
  }

  @Override
  public ChannelWebhookResult handle(ChannelWebhookRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    return switch (request.method()) {
      case "GET" -> handleVerification(request);
      case "POST" -> handleCallback(request);
      default -> new ChannelWebhookResult(
          List.of(),
          new ChannelWebhookResponse(
              405,
              "text/plain; charset=utf-8",
              "method not allowed",
              Map.of("Allow", "GET, POST")));
    };
  }

  private ChannelWebhookResult handleVerification(ChannelWebhookRequest request) {
    String echo = requireText(request.firstQueryParameter("echostr"), "echostr");
    return new ChannelWebhookResult(
        List.of(),
        new ChannelWebhookResponse(200, "text/plain; charset=utf-8", echo, Map.of()));
  }

  private ChannelWebhookResult handleCallback(ChannelWebhookRequest request) {
    Element payload = parsePayload(requireText(request.body(), "body"));
    String messageType = requireText(elementText(payload, "MsgType"), "MsgType");
    if (!"text".equalsIgnoreCase(messageType)) {
      return new ChannelWebhookResult(List.of(), SUCCESS_RESPONSE);
    }
    String content = normalize(elementText(payload, "Content"));
    if (content == null) {
      return new ChannelWebhookResult(List.of(), SUCCESS_RESPONSE);
    }
    String fromUserName = requireText(elementText(payload, "FromUserName"), "FromUserName");
    String toUserName = requireText(elementText(payload, "ToUserName"), "ToUserName");
    String chatId = normalize(elementText(payload, "ChatId"));
    String conversationId = firstNonBlank(chatId, fromUserName);
    String createTime = requireText(elementText(payload, "CreateTime"), "CreateTime");
    String messageId = firstNonBlank(normalize(elementText(payload, "MsgId")), fromUserName + ":" + createTime);
    String agentId = normalize(elementText(payload, "AgentID"));
    Map<String, String> targetAttributes = new LinkedHashMap<>();
    putIfPresent(targetAttributes, "toUserName", toUserName);
    putIfPresent(targetAttributes, "agentId", agentId);
    putIfPresent(targetAttributes, "chatId", chatId);
    Map<String, Object> metadata = new LinkedHashMap<>();
    putIfPresent(metadata, "msgType", messageType.toLowerCase(java.util.Locale.ROOT));
    putIfPresent(metadata, "toUserName", toUserName);
    putIfPresent(metadata, "agentId", agentId);
    putIfPresent(metadata, "chatId", chatId);
    putIfPresent(metadata, "messageCreatedAt", Instant.ofEpochSecond(Long.parseLong(createTime)));
    return new ChannelWebhookResult(
        List.of(new ChannelInboundMessage(
            messageId,
            accountProfile.id(),
            ChannelType.WECOM,
            new ChannelTarget(
                accountProfile.id(),
                conversationId,
                null,
                fromUserName,
                Map.copyOf(targetAttributes)),
            new ChannelParticipant(fromUserName, null, false, Map.of()),
            content,
            Map.copyOf(metadata))),
        SUCCESS_RESPONSE);
  }

  private static Element parsePayload(String body) {
    try {
      DocumentBuilderFactory factory = secureDocumentBuilderFactory();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new ThrowingErrorHandler());
      Document document = builder.parse(new InputSource(new StringReader(body)));
      document.getDocumentElement().normalize();
      return document.getDocumentElement();
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      throw new IllegalArgumentException("invalid WeCom callback payload", ex);
    }
  }

  private static DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setExpandEntityReferences(false);
    factory.setXIncludeAware(false);
    factory.setNamespaceAware(false);
    return factory;
  }

  private static String elementText(Element root, String tagName) {
    NodeList nodes = root.getElementsByTagName(tagName);
    if (nodes == null || nodes.getLength() == 0 || nodes.item(0) == null) {
      return null;
    }
    return normalize(nodes.item(0).getTextContent());
  }

  private static String firstNonBlank(String first, String second) {
    String normalizedFirst = normalize(first);
    if (normalizedFirst != null) {
      return normalizedFirst;
    }
    return normalize(second);
  }

  private static void putIfPresent(Map<String, String> target, String key, String value) {
    String normalizedValue = normalize(value);
    if (normalizedValue != null) {
      target.put(key, normalizedValue);
    }
  }

  private static void putIfPresent(Map<String, Object> target, String key, Object value) {
    if (value != null) {
      target.put(key, value);
    }
  }

  private static final class ThrowingErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }
}
