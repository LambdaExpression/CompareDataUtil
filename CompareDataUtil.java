import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据比较管理工具
 * <br>
 * 使用场景：
 *       要修改A数据库的一组PID=233的数据，
 *    而A数据库的数据与B数据库的数据关联，
 *    不能先全部删除A数据库的这组数据再新增。
 *    需要知道哪些数据需要新增，哪些需要删除，
 *    哪些需要更新。
 *       另外需要考虑，用户删除了一条数据，又
 *    已新增的方式，把原数据重新添加。导致数据
 *    关联失效的问题。
 * 
 * Created by LambdaExpression on 2016/11/17.
 */
public class CompareDataUtil {

    /**
     * 比较新旧数据
     *
     * @param newData 新数据
     * @param oldData 旧数据
     * @param getId   T::getId
     * @return 比较结果 {"add":{},"update":{},"updateBefore":{},"delete":{}}
     */
    private static <T extends Serializable> Map<String, List<T>> compareData(List<T> newData, List<T> oldData,
                                                                             Function<? super T, ?> getId) {
        Map<String, List<T>> result = new HashMap();
        Map<Object, T> update = newData.stream().filter(i -> getId.apply(i) != null).collect(Collectors.toMap(getId, i -> i));
        result.put("add", newData.stream().filter(i -> getId.apply(i) == null).collect(Collectors.toList()));
        result.put("update", new ArrayList<T>(update.values()));
        result.put("updateBefore", oldData.stream().filter(i -> update.get(getId.apply(i)) != null).collect(Collectors.toList()));
        result.put("delete", oldData.stream().filter(i -> update.get(getId.apply(i)) == null).collect(Collectors.toList()));
        return result;
    }

    /**
     * 比较新旧数据(支持通过另一个唯一值T::getCommon，判断哪些是用户伪新增操作)
     *
     * @param newData   新数据
     * @param oldData   旧数据
     * @param getId     T::getId
     * @param getCommon T::getCommon
     * @return 比较结果 {"add":{},"update":{},"updateBefore":{},"delete":{}}
     */
    private static <T extends Serializable> Map<String, List<T>> compareData(List<T> newData, List<T> oldData,
                                                                             Function<? super T, ?> getId, Function<? super T, ?> getCommon) {
        //1.设置参数
        Map<String, List<T>> result = new HashMap();
        Map<Object, T> add = newData.stream().filter(i -> getId.apply(i) == null).collect(Collectors.toMap(getCommon, i -> i));
        Map<Object, T> update = newData.stream().filter(i -> getId.apply(i) != null).collect(Collectors.toMap(getId, i -> i));
        Map<Object, T> updateBefore = oldData.stream().filter(i -> update.get(getId.apply(i)) != null).collect(Collectors.toMap(getId, i -> i));
        Map<Object, T> delete = oldData.stream().filter(i -> update.get(getId.apply(i)) == null).collect(Collectors.toMap(getCommon, i -> i));
        //2.比较添加的和删除的属性是否一样
        List<Object> deleteKey = new ArrayList<>();
        for (Object key : add.keySet()) {
            if (delete.get(key) != null) {
                T up = add.get(key);
                if (up instanceof DateDTO) {
                    ((DateDTO) up).setId(((DateDTO) (delete.get(key))).getId());
                } else if (up instanceof DateDTO) {
                    ((DateDTO) up).setId(((DateDTO) (delete.get(key))).getId());
                } else {
                    continue;
                }
                update.put(getId.apply(delete.get(key)), up);
                updateBefore.put(getId.apply(delete.get(key)), delete.get(key));
                deleteKey.add(key);
            }
        }
        for (Object key : deleteKey) {
            add.remove(key);
            delete.remove(key);
        }
        //3.保存数据
        result.put("add", new ArrayList<T>(add.values()));
        result.put("update", new ArrayList<T>(update.values()));
        result.put("updateBefore", new ArrayList<T>(updateBefore.values()));
        result.put("delete", new ArrayList<T>(delete.values()));
        return result;
    }

    public static void main(String[] args) {

        List<DateDTO> oldDate = new ArrayList<>();
        DateDTO s1 = new DateDTO();
        s1.setId(1);
        s1.setName("123");
        oldDate.add(s1);
        DateDTO s2 = new DateDTO();
        s2.setId(2);
        s2.setName("789");
        s2.setRemark("123");
        oldDate.add(s2);
        DateDTO s3 = new DateDTO();
        s3.setId(3);
        s3.setName("456");
        oldDate.add(s3);


        List<DateDTO> newDate = new ArrayList<>();
        DateDTO s4 = new DateDTO();
        s4.setId(1);
        s4.setName("123123");
        newDate.add(s1);
        DateDTO s5 = new DateDTO();
        s5.setName("789");
        s5.setRemark("432");
        newDate.add(s5);
        DateDTO s6 = new DateDTO();
        s6.setId(3);
        s6.setName("456");
        newDate.add(s6);
        DateDTO s7 = new DateDTO();
        s7.setName("4567");
        newDate.add(s7);

        System.out.println(CompareDataUtil.compareData(newDate, oldDate, DateDTO::getId, DateDTO::getName));
        System.out.println(CompareDataUtil.compareData(newDate, oldDate, DateDTO::getId));
    }

}
