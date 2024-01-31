package io.antmedia.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import io.antmedia.plugin.FaceSwapPlugin;
import io.antmedia.plugin.api.IFrameListener;
import io.antmedia.plugin.api.StreamParametersInfo;

//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferByte;
//import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;

//import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.bytedeco.ffmpeg.global.avutil.*;


public class FaceSwapFrameListener implements IFrameListener{
	
	protected static Logger logger = LoggerFactory.getLogger(FaceSwapFrameListener.class);

	private int audioFrameCount = 0;
	private int videoFrameCount = 0;

	private int width=0;
	private int height=0;
	private int format=0;


	BytePointer inputData=null;

	byte[] inputBytes;
	byte[] decodedBytes;
	String encoded;

    HttpClient client = HttpClient.newHttpClient();
	HttpRequest request=null;
	HttpResponse<String> response=null;

	ByteBuffer yPlaneBuffer = null;
	ByteBuffer uPlaneBuffer = null;
	ByteBuffer vPlaneBuffer = null;

	

	@Override
	public AVFrame onAudioFrame(String streamId, AVFrame audioFrame) {
		audioFrameCount ++;
		return audioFrame;
	}

	@Override
	public AVFrame onVideoFrame(String streamId, AVFrame videoFrame) {
		videoFrameCount ++;
//		long startTime = System.nanoTime();
		width=videoFrame.width();
		height=videoFrame.height();
		format=videoFrame.format();
		if(format==0){
            int size = width * height * 3 / 2;
			inputData=videoFrame.data(0);
            ByteBuffer inputBuffer = inputData.position(0).limit(size).asBuffer();
			if (inputBuffer.hasArray()) {
				inputBytes = inputBuffer.array();
			} else {
				inputBytes = new byte[inputBuffer.remaining()];
				inputBuffer.get(inputBytes);
			}
			encoded = Base64.getEncoder().encodeToString(inputBytes);
//			long encodeTime = System.nanoTime();

			var values = new HashMap<String, String>() {{
				put("data", encoded);
				put("height",String.valueOf(height));
				put("width",String.valueOf(width));
				put("format",String.valueOf(format));
			}};

			var objectMapper = new ObjectMapper();
			String requestBody;
			try {
				requestBody = objectMapper
						.writeValueAsString(values);
			} catch (JsonProcessingException e) {
				logger.error("FaceSwap Failed in making requestBody Returning original frame.");
				logger.error(e.toString());
				return videoFrame;
			}


			request = HttpRequest.newBuilder()
					.uri(URI.create("http://192.168.149.195:5000"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.build();

			try {
				response = client.send(request,
						HttpResponse.BodyHandlers.ofString());
			} catch (IOException | InterruptedException e) {
				logger.error("FaceSwap Failed in communication with server. Returning original frame.");
				logger.error(e.toString());
				return videoFrame;
			}
//			long httpTime = System.nanoTime();
			try{
				decodedBytes = Base64.getDecoder().decode(response.body());
//				decodedBytes=Base64.getDecoder().decode(encoded);
			}catch(Exception e){
				logger.error("FaceSwap Failed in decoding. Returning original frame.");
				logger.error(e.toString());
				return videoFrame;
			}

			if (decodedBytes != null) {
				int yPlaneSize = width * height;
				int uvPlaneSize = width * height / 4;  // Assuming chroma subsampling 4:2:0

				yPlaneBuffer = ByteBuffer.wrap(decodedBytes, 0, yPlaneSize);
				uPlaneBuffer = ByteBuffer.wrap(decodedBytes, yPlaneSize, uvPlaneSize);
				vPlaneBuffer = ByteBuffer.wrap(decodedBytes, yPlaneSize + uvPlaneSize, uvPlaneSize);


				try(BytePointer yPointer = new BytePointer(yPlaneBuffer);
					BytePointer uPointer = new BytePointer(uPlaneBuffer);
					BytePointer vPointer = new BytePointer(vPlaneBuffer)){
//					videoFrame.data(0,yPointer);
//					videoFrame.data(1, uPointer);
//					videoFrame.data(2, vPointer);
//					logger.info("FaceSwap Successful");
//					return videoFrame;
					try (AVFrame newVideoFrame = av_frame_clone(videoFrame)) {
						newVideoFrame.data(0,yPointer);
						newVideoFrame.data(1, uPointer);
						newVideoFrame.data(2, vPointer);
//						long endTime = System.nanoTime();
//						long encodeDuration = (encodeTime - startTime);
//						long httpDuration=(httpTime-encodeTime);
//						long decodeDuration=(endTime-httpTime);
//						logger.info("FaceSwap Successful. Encode Time===> "+
//								Long.toString(encodeDuration)+"ns, Http Time===>"+Long.toString(httpDuration)
//								+"ns, Decode TIme===>"+Long.toString(decodeDuration)+"ns");
//						logger.info("FaceSwap Successful");
						return newVideoFrame;
					}

				}catch(Exception e){
					logger.error("FaceSwap Failed in making new pointer. Returning original frame.");
					logger.error(e.toString());
					return videoFrame;
				}
			} else {
				logger.error("FaceSwap Failed in decoding. DecodedBytes is null. Returning original frame.");
				return videoFrame;
			}
		}
		else{
			return videoFrame;
		}

	}

	@Override
	public void writeTrailer(String streamId) {
		logger.info("FaceSwapFrameListener.writeTrailer() for streamId:{}", streamId);
	}

	@Override
	public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {
		logger.info("FaceSwapFrameListener.setVideoStreamInfo() for streamId:{}", streamId);
	}

	@Override
	public void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo) {
		logger.info("FaceSwapFrameListener.setAudioStreamInfo() for streamId:{}", streamId);
	}

	@Override
	public void start() {
		logger.info("FaceSwapFrameListener.start()");
	}

	public String getStats() {
		return "audio frames:"+audioFrameCount+"\t"+"video frames:"+videoFrameCount;
	}


}
