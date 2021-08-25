package com.demo.music.test;

import com.demo.music.model.Artist;
import com.demo.music.model.Song;
import com.demo.music.service.SongCrawlerService;
import com.demo.music.service.impl.SongCrawlerServiceImpl;

/**
 * @author FiveMountain
 * @date 2021/4/3 18:21
 */
public class SongCrawlerTest {
    private static final String SINGER = "萨顶顶";
    private static final String ARTIST_ID = "9270";
    private static final String ARTIST_NAME = "香蜜沉沉烬如霜 电视原声音乐专辑";
    private static final String SONG_ID = "536096151";
    private static final String SONG_NAME = "左手指月";

    public static void main(String[] args) {
        SongCrawlerService songService = new SongCrawlerServiceImpl();
        songService.start(ARTIST_ID);

        Artist artist = songService.getArtist(ARTIST_ID);
        System.out.println("歌单名称：" + artist.getName());
        if (!SINGER.equals(artist.getName())) {
            System.out.println("歌单名称错误，不是本测试用例指定的歌单。");
            System.exit(1);
        }

        Song song = songService.getSong(ARTIST_ID, SONG_ID);
        System.out.println("歌曲名称：" + song.getName());
        if (!SONG_NAME.equals(song.getName())) {
            System.out.println("歌曲名称错误，不是本测试用例指定的歌曲。");
            System.exit(1);
        }

        if (!SINGER.equals(song.getSingers().get(0).getNickName())) {
            System.out.println("歌曲名称错误，不是本测试用例指定的歌曲。");
            System.exit(1);
        }

        if (!ARTIST_NAME.equals(song.getAlbum().getName())) {
            System.out.println("专辑名称错误，不是本测试用例指定的歌曲的专辑。");
            System.exit(1);
        }

        if (song.getSourceUrl() == null) {
            System.out.println("歌曲名称错误，不是本测试用例指定的歌曲。");
            System.exit(1);
        }

        if (song.getHotComments() == null || song.getHotComments().isEmpty()) {
            System.out.println("歌曲热门评论错误，没有正确抓取评论数据。");
            System.exit(1);
        }

        System.out.println("歌曲所属专辑名称：" + song.getAlbum().getName());
        System.out.println("歌曲的歌手名称：" + song.getSingers().get(0).getNickName());
        System.out.println("歌曲音乐为文件地址：" + song.getSourceUrl());
        System.out.println("歌曲热门评论：" + song.getHotComments().get(0).getContent());

        System.out.println("歌曲服务运行成功。非常棒！");
        System.exit(0);
    }
}
