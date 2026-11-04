package com.chess2.service;

import com.chess2.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;

final class ChessBoard {
    private ChessBoard() { }
    static String initial(ObjectMapper mapper) { return write(mapper, new String[][] {{"bR","bN","bB","bQ","bK","bB","bN","bR"},{"bP","bP","bP","bP","bP","bP","bP","bP"},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{"wP","wP","wP","wP","wP","wP","wP","wP"},{"wR","wN","wB","wQ","wK","wB","wN","wR"}}); }
    static String move(ObjectMapper mapper, String json, String from, String to, String turn) {
        String[][] board = read(mapper, json); int[] start=square(from); int[] end=square(to); String piece=board[start[0]][start[1]];
        char colour = turn.equals("WHITE") ? 'w' : 'b';
        if (piece==null || piece.charAt(0)!=colour) throw new ApiException(HttpStatus.CONFLICT, "Select a piece belonging to the current player");
        if (!legal(board,start[0],start[1],end[0],end[1])) throw new ApiException(HttpStatus.BAD_REQUEST, "Illegal move");
        board[end[0]][end[1]] = piece.charAt(1)=='P' && (end[0]==0 || end[0]==7) ? piece.charAt(0)+"Q" : piece; board[start[0]][start[1]]=null; return write(mapper,board);
    }
    static List<List<String>> view(ObjectMapper mapper, String json) { String[][] board=read(mapper,json); List<List<String>> rows=new ArrayList<>(); for(String[] row:board) rows.add(Arrays.asList(row)); return rows; }
    static String botMove(ObjectMapper mapper, String json) { for(char file='a'; file<='h'; file++) for(char rank='1'; rank<='8'; rank++) for(char targetFile='a'; targetFile<='h'; targetFile++) for(char targetRank='1'; targetRank<='8'; targetRank++) { try { return move(mapper, json, ""+file+rank, ""+targetFile+targetRank, "BLACK"); } catch (ApiException ignored) { } } throw new ApiException(HttpStatus.CONFLICT, "Bot has no available move"); }
    private static boolean legal(String[][] b,int fr,int fc,int tr,int tc) { if(fr==tr&&fc==tc)return false; String p=b[fr][fc], target=b[tr][tc]; if(target!=null&&(target.charAt(0)==p.charAt(0)||target.charAt(1)=='K'))return false; int dr=tr-fr,dc=tc-fc; return switch(p.charAt(1)){case 'P' -> pawn(b,p.charAt(0),fr,fc,tr,tc);case 'N' -> Math.abs(dr)*Math.abs(dc)==2;case 'K' -> Math.max(Math.abs(dr),Math.abs(dc))==1;case 'B' -> Math.abs(dr)==Math.abs(dc)&&clear(b,fr,fc,tr,tc);case 'R' -> (dr==0||dc==0)&&clear(b,fr,fc,tr,tc);case 'Q' -> (dr==0||dc==0||Math.abs(dr)==Math.abs(dc))&&clear(b,fr,fc,tr,tc);default -> false;}; }
    private static boolean pawn(String[][] b,char colour,int fr,int fc,int tr,int tc){int direction=colour=='w'?-1:1,dr=tr-fr,dc=Math.abs(tc-fc);if(dc==0&&b[tr][tc]==null){if(dr==direction)return true;int start=colour=='w'?6:1;return fr==start&&dr==2*direction&&b[fr+direction][fc]==null;}return dc==1&&dr==direction&&b[tr][tc]!=null&&b[tr][tc].charAt(0)!=colour;}
    private static boolean clear(String[][] b,int fr,int fc,int tr,int tc){int rs=Integer.compare(tr,fr),cs=Integer.compare(tc,fc),r=fr+rs,c=fc+cs;while(r!=tr||c!=tc){if(b[r][c]!=null)return false;r+=rs;c+=cs;}return true;}
    private static int[] square(String s){return new int[]{'8'-s.charAt(1),s.charAt(0)-'a'};}
    private static String[][] read(ObjectMapper mapper,String json){try{return mapper.readValue(json,String[][].class);}catch(JsonProcessingException e){throw new IllegalStateException("Stored game board is invalid",e);}}
    private static String write(ObjectMapper mapper,String[][] board){try{return mapper.writeValueAsString(board);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
}
