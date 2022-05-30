package edu.icewiz.crdt;

import org.java_websocket.exceptions.IncompleteException;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CrdtDoc {
    List<CrdtItem> content;
    int length;
    HashMap<String,Integer> version;
    ArrayList<CrdtItem> WaitListInsert;
    ArrayList<CrdtItem> WaitListDelete;
    ReentrantLock lock = new ReentrantLock();
    public CrdtDoc(){
        content = Collections.synchronizedList(new ArrayList());
        length = 0;
        version = new HashMap<>();
        WaitListInsert = new ArrayList<>(0);
        WaitListDelete = new ArrayList<>(0);
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

    public boolean isInDoc(ItemID id){
        if(id == null || id.agent == null)return true;
        return version.get(id.agent) != null && version.get(id.agent) >= id.seq;
    }

    boolean shouldInsertNow(CrdtItem item){
        return !isInDoc(item.id) &&
                (item.id.seq == 0 || isInDoc(new ItemID(item.id.agent, item.id.seq - 1))) &&
                isInDoc(item.originLeft) && isInDoc(item.originRight);
    }

    void TryClearWaitListInsert(){
        while(WaitListInsert.size() > 0) {
            boolean shouldContinue = false;
            ArrayList<CrdtItem> CannotInsert = new ArrayList<>(0);
            for (CrdtItem item : WaitListInsert) {
                if (shouldInsertNow(item)) {
                    Insert(item);
                    shouldContinue = true;
                } else {
                    if (isInDoc(item.id)) continue;
                    CannotInsert.add(item);
                }
            }
            WaitListInsert = CannotInsert;
            if (shouldContinue == false) break;
        }
    }

    void TryClearWaitListDelete(){
        ArrayList<CrdtItem> CannotDelete = new ArrayList<>(0);
        for(CrdtItem item: WaitListDelete){
            if(isInDoc(item.id)){
                Delete(item);
            }else{
                CannotDelete.add(item);
            }
        }
        WaitListDelete = CannotDelete;
    }

    public void addInsertOperationToWaitList(CrdtItem item){
        if(isInDoc(item.id))return;
        WaitListInsert.add(item);
        TryClearWaitListInsert();
        TryClearWaitListDelete();
    }

    public void addDeleteOperationToWaitList(CrdtItem item){
        if(isInDoc(item.id))Delete(item);
        else WaitListDelete.add(item);
    }

    public CrdtItem localInsert(String agent, int pos, String value){
        int i = findItemAtPos(pos);
        CrdtItem item =
                new CrdtItem(value,
                    new ItemID(agent,getNextSeq(agent)),
                    getItemIDAtPos(i - 1),
                    getItemIDAtPos(i),
                    false
                );
        integrate(item , i);
        return item;
    }

    public CrdtItem localDelete(String agent, int pos){
        int i = findItemAtPos(pos);
        if(i >= content.size())return null;
        CrdtItem item = content.get(i);
        if(!item.isDeleted){
            item.isDeleted = true;
            length--;
        }
        return item;
    }

    public void Insert(CrdtItem item){
        integrate(item, -1);
    }
    public void Delete(CrdtItem item){
        int pos = findItem(item.id, -1);
        CrdtItem myItem = content.get(pos);
        if(!myItem.isDeleted){
            myItem.isDeleted = true;
            length--;
        }
    }

    void integrate(CrdtItem item, int idx_hint) {
        int shouldProcessSeq = getNextSeq(item.id.agent);
        if(shouldProcessSeq != item.id.seq){
            System.out.println(String.format("Should see operation seq #%v, but saw #%v instead", shouldProcessSeq, item.id.seq));
            return;
        }
//        System.out.println(item.id.agent);
        version.put(item.id.agent, item.id.seq);
//        if(item.originLeft != null)System.out.println(item.originLeft.agent);
        int left = findItem(item.originLeft, idx_hint - 1);
//        System.out.println(left);
        int destIdx = left + 1;
        int right = item.originRight == null ? content.size() : findItem(item.originRight, idx_hint);
        boolean scanning = false;
//        System.out.println(right);
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

    public List<CrdtItem> returnCopy(){
        try{
            lock.lock();
            return content;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        try{
            lock.lock();
            StringBuilder result = new StringBuilder();
            for(CrdtItem item: content){
                if(item.isDeleted == false){
                    result.append(item.value);
                }
            }
            return result.toString();
        }finally {
            lock.unlock();
        }

    }
}
