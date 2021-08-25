package com.demo.music.service.impl;

import com.alibaba.fastjson.JSON;
import com.demo.music.model.*;
import com.demo.music.service.SongCrawlerService;
import com.demo.music.util.WordCloudUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.*;


/**
 * @author FiveMountain
 * @date 2021/4/3 18:20
 */
public class SongCrawlerServiceImpl implements SongCrawlerService {
    /**
     * 歌单API
     */
    private static final String ARTIEST_API_PREFIX = "http://neteaseapi.youkeda.com:3000/artists?id=";
    /**
     * 歌曲详情API
     */
    private static final String S_D_API_PREFIX = "http://neteaseapi.youkeda.com:3000/song/detail?ids=";
    /**
     * 歌曲评论 API
     */
    private static final String S_C_API_PREFIX = "http://neteaseapi.youkeda.com:3000/comment/music?id=";
    /**
     * 歌曲音乐文件 API
     */
    private static final String S_F_API_PREFIX = "http://neteaseapi.youkeda.com:3000/song/url?id=";
    /**
     * okHttpClient 实例
     */
    private final OkHttpClient okHttpClient;
    /**
     * 歌单数据仓库
     */
    private final HashMap<String, Artist> artists;

    public SongCrawlerServiceImpl() {
        okHttpClient = new OkHttpClient();
        artists = new HashMap<>();
    }

    @Override
    public void start(String artistId) {
        // 参数判断，未输入参数则直接返回
        if (artistId == null || "".equals(artistId)) {
            return;
        }
        // 初始化歌单及数据
        initArtistHotSongs(artistId);
        assembleSongDetail(artistId);
        assembleSongComment(artistId);
        assembleSongUrl(artistId);
        generateWordCloud(artistId);
    }

    @Override
    public Artist getArtist(String artistId) {
        return artists.get(artistId);
    }

    @Override
    public Song getSong(String artistId, String songId) {
        Artist artist = artists.get(artistId);
        List<Song> songs = artist.getSongList();
        if (songs == null) {
            return null;
        }
        for (Song song : songs) {
            if (songId.equals(song.getId())) {
                return song;
            }
        }
        return null;
    }

    private void initArtistHotSongs(String artistId) {
        // 取得整体数据对象
        var returnData = getSourceDataObj(ARTIEST_API_PREFIX, artistId);
        // 构建填充了属性的Artist实例
        Artist artist = buildArtist(returnData);
        // 构建一组填充了属性的Song实例
        List<Song> songs = buildSongs(returnData);
        // 歌曲填入歌单
        artist.setSongList(songs);
        // 存入本地
        artists.put(artist.getId(), artist);
    }

    @SuppressWarnings("unchecked")
    private void assembleSongDetail(String artistId) {
        Artist artist = getArtist(artistId);
        // 取不到歌单说明参数输入错误
        if (artist == null) { return; }
        // 删除其它语句，保留必要的语句
        List<Song> songs = artist.getSongList();
        String sIdsParam = buildManyIdParam(songs);
        // 抓取结果
        HashMap<?, ?> songsDetailObj = getSourceDataObj(S_D_API_PREFIX, sIdsParam);
        // 原始数据中的 songs 是歌曲列表
        List<HashMap<?, ?>> sourceSongs = (List<HashMap<?, ?>>) songsDetailObj.get("songs");
        // 临时的 HashMap
        HashMap<String, HashMap<?, ?>> sourceSongsMap = new HashMap<>(16);
        // 遍历歌曲列表
        for (HashMap<?, ?> songSourceData : sourceSongs) {
            String sId = songSourceData.get("id").toString();
            // 原始歌曲数据对象放入一个临时的 Map 中
            sourceSongsMap.put(sId, songSourceData);
        }

        // 再次遍历歌单中的歌曲，填入详情数据
        for (Song song : songs) {
            String sId = song.getId();
            // 从临时的HashMap中取得对应的歌曲源数据，使用id直接获取，比较方便
            HashMap<?, ?> songSourceData = sourceSongsMap.get(sId);
            // 源歌曲数据中，ar 字段是歌手列表
            List<HashMap<?, ?>> singersData = (List<HashMap<?, ?>>) songSourceData.get("ar");
            // 歌手集合
            List<User> singers = new ArrayList<>();
            for (HashMap<?, ?> singerData : singersData) {
                // 歌手对象
                User singer = new User();
                singer.setId(singerData.get("id").toString());
                singer.setNickName(singerData.get("name").toString());
                // 歌手集合放入歌手对象
                singers.add(singer);
            }
            // 歌手集合放入歌曲
            song.setSingers(singers);

            // 专辑
            HashMap<?, ?> albumData = (HashMap<?, ?>) songSourceData.get("al");
            Album album = new Album();
            album.setId(albumData.get("id").toString());
            album.setName(albumData.get("name").toString());
            if (albumData.get("picUrl") != null) {
                album.setPicUrl(albumData.get("picUrl").toString());
            }
            // 专辑对象放入歌曲
            song.setAlbum(album);
        }
    }

    @SuppressWarnings("unchecked")
    private void assembleSongComment(String artistId) {
        Artist artist = getArtist(artistId);
        // 取不到歌单说明参数输入错误
        if (artist == null) {
            return;
        }

        List<Song> songs = artist.getSongList();
        for (Song song : songs) {
            String sIdsParam = song.getId() + "&limit=5";
            // 抓取结果
            HashMap<?, ?> songsCommentObj = getSourceDataObj(S_C_API_PREFIX, sIdsParam);
            // 热门评论列表
            List<HashMap<?, ?>> hotCommentsObj = (List<HashMap<?, ?>>) songsCommentObj.get("hotComments");
            // 评论列表
            List<HashMap<?, ?>> commentsObj = (List<HashMap<?, ?>>) songsCommentObj.get("comments");

            song.setHotComments(buildComments(hotCommentsObj));
            song.setComments(buildComments(commentsObj));
        }
    }

    @SuppressWarnings("unchecked")
    private void assembleSongUrl(String artistId) {
        Artist artist = getArtist(artistId);
        // 取不到歌单说明参数输入错误
        if (artist == null) { return; }
        // 删除其它语句，保留必要的语句
        List<Song> songs = artist.getSongList();
        String sIdsParam = buildManyIdParam(songs);
        // 抓取结果
        HashMap<?, ?> songsDetailObj = getSourceDataObj(S_F_API_PREFIX, sIdsParam);
        // 原始数据中的 data 是音乐文件列表
        List<HashMap<?, ?>> data = (List<HashMap<?, ?>>) songsDetailObj.get("data");
        // 临时的 Map
        HashMap<String, HashMap<?, ?>> sourceSongsMap = new HashMap<>(16);
        // 遍历音乐文件列表
        for (HashMap<?, ?> songFileData : data) {
            String sId = songFileData.get("id").toString();
            // 原始音乐文件数据对象放入一个临时的 HashMap 中
            sourceSongsMap.put(sId, songFileData);
        }

        // 再次遍历歌单中的歌曲，填入音乐文件URL
        for (Song song : songs) {
            String sId = song.getId();
            // 从临时的HashMap中取得对应的音乐文件源数据，使用id直接获取，比较方便
            HashMap<?, ?> songFileData = sourceSongsMap.get(sId);
            // 源音乐文件数据中，url 字段就是文件地址
            if (songFileData != null && songFileData.get("url") != null) {
                String songFileUrl = songFileData.get("url").toString();
                song.setSourceUrl(songFileUrl);
            }
        }
    }

    private void generateWordCloud(String artistId) {
        Artist artist = getArtist(artistId);
        // 取不到歌单说明参数输入错误
        if (artist == null) {
            return;
        }

        List<Song> songs = artist.getSongList();
        List<String> contents = new ArrayList<>();
        for (Song song : songs) {
            // 遍历歌曲所有的评论，包括普通评论和热门评论，把评论内容字符串存入列表
            collectContent(song.getComments(), contents);
            collectContent(song.getHotComments(), contents);
        }

        // 制作词云
        WordCloudUtil.generate(artistId, contents);
    }

    private HashMap<?, ?> getSourceDataObj(String prefix, String postfix) {
        // 构建歌单URL
        String artistUrl = prefix + postfix;
        // 调用okhttp3获取返回数据
        // 定义一个request
        Request request = new Request.Builder().url(artistUrl).build();
        // 使用client去请求
        Call call = okHttpClient.newCall(request);
        String content = null;
        try {
            // 获得返回结果
            content = Objects.requireNonNull(call.execute().body()).string();
            System.out.println("call " + artistUrl + ", content's size = " + content.length());
        } catch (IOException e) {
            System.out.println("request " + artistUrl + " error.");
            e.printStackTrace();
        }
        // 反序列化成HashMap对象并返回
        return JSON.parseObject(content, HashMap.class);
    }

    @SuppressWarnings("unchecked")
    private Artist buildArtist(HashMap<?, ?> returnData) {
        // 从HashMap对象中取得歌单数据，歌单也是一个子Map对象
        HashMap<?, ?> artistData = (HashMap<?, ?>) returnData.get("artist");
        Artist artist = new Artist();
        artist.setId(artistData.get("id").toString());
        if (artistData.get("picUrl") != null) {
           artist.setPicUrl(artistData.get("picUrl").toString());
        }
        artist.setBriefDesc(artistData.get("briefDesc").toString());
        artist.setImg1v1Url(artistData.get("img1v1Url").toString());
        artist.setName(artistData.get("name").toString());
        artist.setAlias((List<String>) artistData.get("alias"));

        return artist;
    }

    private List<Song> buildSongs(HashMap<?, ?> returnData) {
        // 从HashMap对象中取得一组歌曲数据
        List<?> songsData = (List<?>) returnData.get("hotSongs");
        List<Song> songs = new ArrayList<>();

        for (Object songsDatum : songsData) {
            HashMap<?, ?> songData = (HashMap<?, ?>) songsDatum;
            Song songObj = new Song();
            songObj.setId(songData.get("id").toString());
            songObj.setName(songData.get("name").toString());

            songs.add(songObj);
        }

        return songs;
    }

    /**
     * 收集所有歌曲的ID
     * @param songs 一个歌单中所有歌曲组成的list
     * @return 所有歌曲ID组成的字符串，由‘,’分隔
     */
    private String buildManyIdParam(List<Song> songs) {
        List<String> songIds = new ArrayList<>();
        for (Song song : songs) {
            songIds.add(song.getId());
        }

        return String.join(",", songIds);
    }

    private List<Comment> buildComments(List<HashMap<?, ?>> commentsObj) {
        List<Comment> comments = new ArrayList<>();

        for (HashMap<?, ?> sourceComment : commentsObj) {
            Comment comment = new Comment();
            comment.setContent(sourceComment.get("content").toString());
            comment.setId(sourceComment.get("commentId").toString());
            comment.setLikedCount(sourceComment.get("likedCount").toString());
            comment.setTime(sourceComment.get("time").toString());

            User user = new User();
            HashMap<?, ?> sourceUserData = (HashMap<?, ?>) sourceComment.get("user");
            user.setId(sourceUserData.get("userId").toString());
            user.setNickName(sourceUserData.get("nickname").toString());
            user.setAvatar(sourceUserData.get("avatarUrl").toString());
            comment.setCommentUser(user);

            comments.add(comment);
        }

        return comments;
    }

    private void collectContent(List<Comment> comments, List<String> contents) {
        for (Comment comment : comments) {
            contents.add(comment.getContent());
        }
    }
}
