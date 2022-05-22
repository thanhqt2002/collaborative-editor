package edu.icewiz.crdt;

import org.java_websocket.exceptions.IncompleteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class CrdtDoc {
    ArrayList<CrdtItem> content;
    int length;
    HashMap<String,Integer> version;

    public CrdtDoc(){
        content = new ArrayList<>(0);
        length = 0;
        version = new HashMap<>();
    }

    boolean idEq(ItemID a, ItemID b){
        return a == b || (a != null && b != null && a.agent.equals(b.agent) && a.seq == b.seq);
    }

    boolean idEq(ItemID a, String agent, int seq){
        return a != null && a.agent.equals(agent) && a.seq == seq;
    }

    boolean foundItem(CrdtItem a, String agent, int seq, boolean atEnd){
        if(a == null)return false;
        return idEq(a.id, agent, seq) && (!atEnd || a.value != null);
    }

    int findItem(ItemID needle, boolean atEnd, int idx_hint) throws NoSuchElementException{
        if(needle == null)return -1;
        String agent = needle.agent;
        int seq = needle.seq;
        if (idx_hint >= 0 && idx_hint < content.size()) {
            CrdtItem hintItem = content.get(idx_hint);
            if(foundItem(hintItem, agent, seq, atEnd)){
                return idx_hint;
            }
        }
        for(int i = 0 ; i < content.size(); ++i){
            if(foundItem(content.get(i), agent, seq, atEnd))return i;
        }
        throw new NoSuchElementException();
    }

    int findItem(ItemID needle, int idx_hint){
        return findItem(needle, false, idx_hint);
    }

    int findItem(ItemID needle){
        return findItem(needle, false, -1);
    }
    int findItemAtPos(int pos, boolean stick_end) throws NoSuchElementException{
        int cntValid = 0;
        int i = 0;
        for(i = 0; i < content.size(); ++i){
            CrdtItem item = content.get(i);
            if(stick_end && pos == cntValid)return i;
            if(item.isDeleted == false && item.value != null) {
                if (pos == cntValid) return i;
                ++cntValid;
            }
        }
        if(pos == cntValid)return i;
        throw new NoSuchElementException();
    }

    int findItemAtPos(int pos){
        return findItemAtPos(pos, false);
    }

    int getNextSeq(String agent){
        return version.get(agent) == null ? 0 : version.get(agent) + 1;
    }

    ItemID getItemIDAtPos(int pos){
        if(pos < 0 || pos >= content.size())return null;
        return content.get(pos).id;
    }

    public void localInsert(String agent, int pos, String value){
        int i = findItemAtPos(pos);
        integrate(new CrdtItem(value,
                new ItemID(agent,getNextSeq(agent)),
                getItemIDAtPos(i - 1),
                getItemIDAtPos(i),
                false
                ), i);
    }

    public void localDelete(String agent, int pos){
        CrdtItem item = content.get(findItemAtPos(pos));
        if(!item.isDeleted){
            item.isDeleted = true;
            length--;
        }
    }

    void integrate(CrdtItem item, int idx_hint) {
        int shouldProcessSeq = getNextSeq(item.id.agent);
        if(shouldProcessSeq != item.id.seq){
            System.out.println(String.format("Should see operation seq #%v, but saw #%v instead", shouldProcessSeq, item.id.seq));
            return;
        }
        System.out.println(item.id.agent);
        version.put(item.id.agent, item.id.seq);
        if(item.originLeft != null)System.out.println(item.originLeft.agent);
        int left = findItem(item.originLeft, idx_hint - 1);
        System.out.println(left);
        int destIdx = left + 1;
        int right = item.originRight == null ? content.size() : findItem(item.originRight, idx_hint);
        boolean scanning = false;
        System.out.println(right);
        for(int i = destIdx; ; ++i){
            if(!scanning)destIdx = i;
            if(i == content.size())break;
            if(i == right)break;
            CrdtItem other = content.get(i);
            int oleft = findItem(other.originLeft, idx_hint - 1);
            int oright = other.originRight == null  ? content.size() : findItem(other.originRight, idx_hint);
            if(oleft < left)break;
            else if(oleft == left){
                if(oright < right){
                    scanning = true;
                    continue;
                }else if(oright == right){
                    if(item.id.agent.compareTo(other.id.agent) > 0)break;
                    else {
                        scanning = false;
                        continue;
                    }
                }else{
                    scanning = false;
                    continue;
                }
            }
        }
        content.add(destIdx, item);
        if(!item.isDeleted)length++;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for(CrdtItem item: content){
            if(item.isDeleted == false){
                result.append(item.value);
            }
        }
        return result.toString();
    }
}
