package models;

import utils.Encryption;
import utils.FileIO;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alberto Delgado on 5/9/22
 * @author anchit bhatia
 * @project dsd-final-project-anchitbhatia
 * <p>
 * Basic torrent data structure.
 * <p>
 * Used this as a template: <a href="https://github.com/m1dnight/torrent-parser/blob/master/src/main/java/be/christophedetroyer/torrent/Torrent.java">https://github.com/m1dnight/torrent-parser/blob/master/src/main/java/be/christophedetroyer/torrent/Torrent.java</a>
 */
public class Torrent {
    public final String announce;                           // tracker announcer
    public final String name;                               // file name (not torrent name)
    public final Long pieceLength;                          // bytes per piece
    public final Map<Long, String> pieces;                  // SHA1 bytes of each piece
    public final boolean singleFileTorrent;                 // is single/multi file
    public final Long totalSize;                            // total size in bytes
    public final List<TorrentFile> fileList;                // list of files (if multi file torrent)
    public final String comment;                            // optional comment about file
    public final String createdBy;                          // uploader "author"
    public final Date creationDate;                         // creation date
    public final List<String> announceList;                 // list of announcers/trackers
    public final String infoHash;                           // SHA256 of entire file (not torrent)
    public List<Long> downloadedPieces = new ArrayList<>(); // Local "owned" pieces
    public final Map<Long, byte[]> piecesCache = new ConcurrentHashMap<>();

    public Torrent(
            String announce,
            String name,
            Long pieceLength,
            Map<Long, String> pieces,
            boolean singleFileTorrent,
            Long totalSize,
            List<TorrentFile> fileList,
            String comment,
            String createdBy,
            Date creationDate,
            List<String> announceList,
            String infoHash
    ) {
        this.announce = announce;
        this.name = name;
        this.pieceLength = pieceLength;
        this.pieces = pieces;
        this.singleFileTorrent = singleFileTorrent;
        this.totalSize = totalSize;
        this.fileList = fileList;
        this.comment = comment;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
        this.announceList = announceList;
        this.infoHash = infoHash;
    }

    /**
     * Torrent name getter
     *
     * @return
     */
    public String getTorrentName() {
        int i = name.indexOf('.');
        return name.substring(0, i) + ".torrent";
    }

    /**
     * Update downloaded pieces
     *
     * @param pieceNumber
     */
    public void addDownloadedPiece(long pieceNumber) {
        downloadedPieces.add(pieceNumber);
    }

    /**
     * Checks if has piece
     *
     * @param pieceNumber
     * @return
     */
    public boolean hasPiece(long pieceNumber) {
        return downloadedPieces.contains(pieceNumber);
    }

    /**
     * To be run once, on library init
     *
     * @throws IOException
     */
    public void checkDownloadedPieces() throws IOException {
        byte[] data = FileIO.getInstance().readFile(name);
        int numberOfPieces = (int) Math.ceil((float) totalSize / pieceLength);
        List<Long> downloadedPieces = new ArrayList<>();
        for (long i = 0; i < numberOfPieces; i += 1) {
            downloadedPieces.add(i);
        }
        this.downloadedPieces = downloadedPieces;
    }

    /**
     * Adds piece to cache
     *
     * @param pieceNumber
     * @param piece
     */
    public void addPieceInCache(Long pieceNumber, byte[] piece) {
        this.piecesCache.put(pieceNumber, piece);
        this.pieces.put(pieceNumber, Arrays.toString(Encryption.encodeSHA256(piece)));
    }

    @Override
    public String toString() {
        return "Torrent{" + '\n' +
                "    announce=" + announce + '\n' +
                "    name=" + name + '\n' +
                "    pieceLength=" + pieceLength + '\n' +
                "    pieces=" + pieces + '\n' +
                "    singleFileTorrent=" + singleFileTorrent + '\n' +
                "    totalSize=" + totalSize + '\n' +
                "    fileList=" + fileList + '\n' +
                "    comment=" + comment + '\n' +
                "    createdBy='" + createdBy + '\n' +
                "    creationDate=" + creationDate + '\n' +
                "    announceList=" + announceList + '\n' +
                "    infoHash='" + infoHash + '\n' +
                '}';
    }

    /**
     * For multiple-file torrent
     */
    public class TorrentFile {
        public final Long fileLength;
        public final List<String> fileDirs;

        public TorrentFile(Long fileLength, List<String> fileDirs) {
            this.fileLength = fileLength;
            this.fileDirs = fileDirs;
        }
    }
}
