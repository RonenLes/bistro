module bistro_Common {
	exports kryo;
	exports responses;
	exports requests;
	requires org.objenesis;
	requires com.esotericsoftware.kryo;
	opens requests to com.esotericsoftware.kryo;
    opens responses to com.esotericsoftware.kryo;    
}