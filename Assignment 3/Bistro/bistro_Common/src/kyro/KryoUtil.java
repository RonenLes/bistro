package kyro;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy; // Make sure you import this!
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoUtil {

 // 1. ThreadLocal ensures every thread gets its own Kryo instance
 private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
     Kryo kryo = new Kryo();
     
     // Disable Unsafe Memory Access
     // This tells Kryo: "Don't use the deprecated Unsafe methods."
     kryo.setRegistrationRequired(false);
     kryo.setReferences(true);
     kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
     
     return kryo;
 });

 public static byte[] serialize(Object object) {
     Kryo kryo = kryoThreadLocal.get();
     ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
     Output output = new Output(outputStream);
     kryo.writeClassAndObject(output, object);
     output.close();
     return outputStream.toByteArray();
 }

 public static Object deserialize(byte[] bytes) {
     if (bytes == null) return null;
     Kryo kryo = kryoThreadLocal.get();
     ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
     Input input = new Input(inputStream);
     return kryo.readClassAndObject(input);
 }
}
