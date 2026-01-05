package com.schemafy.core.common.config.codec;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.MimeType;

import org.reactivestreams.Publisher;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Protobuf 메시지를 JSON 형식으로 디코딩하는 Decoder */
public class ProtobufJsonDecoder implements Decoder<Message> {

  private static final List<MimeType> SUPPORTED_MIME_TYPES = List.of(
      MediaType.APPLICATION_JSON,
      new MediaType("application", "*+json"));

  @Override
  @NonNull
  public List<MimeType> getDecodableMimeTypes() { return SUPPORTED_MIME_TYPES; }

  @Override
  public boolean canDecode(
      @NonNull ResolvableType elementType,
      MimeType mimeType) {
    return Message.class.isAssignableFrom(elementType.toClass())
        && (mimeType == null
            || SUPPORTED_MIME_TYPES.stream()
                .anyMatch(mt -> mt.isCompatibleWith(mimeType)));
  }

  @NonNull
  public Flux<Message> decode(
      @NonNull Publisher<DataBuffer> inputStream,
      @NonNull ResolvableType elementType,
      MimeType mimeType,
      Map<String, Object> hints) {
    return Flux
        .from(decodeToMono(inputStream, elementType, mimeType, hints));
  }

  @NonNull
  public Mono<Message> decodeToMono(
      @NonNull Publisher<DataBuffer> inputStream,
      @NonNull ResolvableType elementType,
      MimeType mimeType,
      Map<String, Object> hints) {

    Flux<DataBuffer> dataBufferFlux = Flux.from(inputStream);

    return DataBufferUtils.join(dataBufferFlux)
        .map(dataBuffer -> {
          try {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);

            String json = new String(bytes, StandardCharsets.UTF_8);

            // protobuf Message.Builder 얻기
            Class<?> messageClass = elementType.toClass();
            Method newBuilderMethod = messageClass
                .getMethod("newBuilder");
            Message.Builder builder = (Message.Builder) newBuilderMethod
                .invoke(null);

            // JSON을 protobuf로 변환
            JsonFormat.parser()
                .ignoringUnknownFields()
                .merge(json, builder);

            return builder.build();

          } catch (IOException e) {
            throw new DecodingException(
                "Failed to parse JSON to Protobuf", e);
          } catch (Exception e) {
            throw new DecodingException(
                "Failed to create Protobuf message", e);
          }
        });
  }

}
