package io.antmedia.app;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.StreamParametersInfo;

public class FaceSwapPacketListener implements IPacketListener{

	private int packetCount = 0;
	
	protected static Logger logger = LoggerFactory.getLogger(FaceSwapPacketListener.class);

	@Override
	public void writeTrailer(String streamId) {
		System.out.println("FaceSwapPacketListener.writeTrailer()");
		
	}

	@Override
	public AVPacket onVideoPacket(String streamId, AVPacket packet) {
		packetCount++;
		return packet;
	}
	
	@Override
	public AVPacket onAudioPacket(String streamId, AVPacket packet) {
		packetCount++;
		return null;
	}

	@Override
	public void setVideoStreamInfo(String streamId, StreamParametersInfo videoStreamInfo) {
		logger.info("FaceSwapPacketListener.setVideoStreamInfo() for streamId:{}", streamId);
	}

	@Override
	public void setAudioStreamInfo(String streamId, StreamParametersInfo audioStreamInfo) {
		logger.info("FaceSwapPacketListener.setAudioStreamInfo() for streamId:{}", streamId);
	}

	public String getStats() {
		return "packets:"+packetCount;
	}

}
