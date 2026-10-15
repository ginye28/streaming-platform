package com.sp.api.live.relay;

/** 방송 하나에서 나오는 fMP4 의 시작 부분(init)과 조각(fragment)을 받는다. */
public interface FragmentListener {

    /**
     * 재생을 시작하는 데 필요한 머리(ftyp+moov). 방송마다 한 번 온다.
     *
     * @param codecs MSE 의 addSourceBuffer 에 그대로 넣는 코덱 문자열(예: avc1.64001f,mp4a.40.2)
     */
    void onInit(String codecs, byte[] init);

    /**
     * 짧은 조각 하나(moof+mdat).
     *
     * @param keyframe 영상 키프레임으로 시작하는 조각이면 true. 늦게 들어온 시청자는 여기서부터 받는다.
     */
    void onFragment(byte[] fragment, boolean keyframe);
}
