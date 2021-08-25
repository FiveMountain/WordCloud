package com.demo.music.service;

import com.demo.music.model.Artist;
import com.demo.music.model.Song;

/**
 * @author FiveMountain
 * @date 2021/4/3 18:19
 */
public interface SongCrawlerService {
    /**
     * 根据歌单ID，抓取歌单数据
     *
     * @param artistId 歌单ID
     */
    public void start(String artistId);

    /**
     * 根据歌单ID，查询歌单对象
     *
     * @param artistId 歌单ID
     * @return 歌单对象
     */
    public Artist getArtist(String artistId);

    /**
     * 根据歌单ID，查询歌曲对象
     *
     * @param artistId 歌单ID
     * @param songId 歌曲ID
     * @return 歌曲对象
     */
    public Song getSong(String artistId, String songId);
}
