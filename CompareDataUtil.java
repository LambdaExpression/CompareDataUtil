
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lin
 * @date 2020-01-14
 */

/**
 * 数据比较管理工具
 * <br>
 * 使用场景：
 * 要修改A数据库的一组PID=233的数据，
 * 而A数据库的数据与B数据库的数据关联，
 * 不能先全部删除A数据库的这组数据再新增。
 * 需要知道哪些数据需要新增，哪些需要删除，
 * 哪些需要更新。
 * 另外需要考虑，用户删除了一条数据，又
 * 已新增的方式，把原数据重新添加。导致数据
 * 关联失效的问题。
 * <p>
 * Created by LambdaExpression on 2016/11/17.
 */
public class CompareDataUtil {

    /**
     * 比较新旧数据
     *
     * @param newData 新数据
     * @param oldData 旧数据
     * @param getId   T::getId
     * @return 比较结果 {Tag.ADD:{},Tag.UPDATE:{},Tag.UPDATE_BEFORE:{},Tag.DELETE:{}}
     */
    private static <T extends Serializable> Map<Tag, List<T>> compareData(List<T> newData, List<T> oldData,
                                                                          Function<? super T, ?> getId) {
        Map<Tag, List<T>> result = new HashMap();
        Map<Object, T> update = newData.stream().filter(i -> getId.apply(i) != null).collect(Collectors.toMap(getId, i -> i));
        result.put(Tag.ADD, newData.stream().filter(i -> getId.apply(i) == null).collect(Collectors.toList()));
        result.put(Tag.UPDATE, new ArrayList<T>(update.values()));
        result.put(Tag.UPDATE_BEFORE, oldData.stream().filter(i -> update.get(getId.apply(i)) != null).collect(Collectors.toList()));
        result.put(Tag.DELETE, oldData.stream().filter(i -> update.get(getId.apply(i)) == null).collect(Collectors.toList()));
        return result;
    }


    /**
     * 比较新旧数据(支持通过另一个唯一值T::getCommon，判断哪些是用户伪新增操作)
     *
     * @param newData   新数据
     * @param oldData   旧数据
     * @param getId     T::getId
     * @param setId     T::setId
     * @param getCommon T::getCommon
     * @return 比较结果 {Tag.ADD:{},Tag.UPDATE:{},Tag.UPDATE_BEFORE:{},Tag.DELETE:{}}
     */
    private static <T extends Serializable, L> Map<Tag, List<T>> compareData(List<T> newData, List<T> oldData,
                                                                             Function<? super T, L> getId, BiConsumer<? super T, L> setId, Function<? super T, ?> getCommon) {
        //1.设置参数
        Map<Tag, List<T>> result = new HashMap();
        Map<Object, T> add = newData.stream().filter(i -> getId.apply(i) == null).collect(Collectors.toMap(getCommon, i -> i));
        Map<Object, T> update = newData.stream().filter(i -> getId.apply(i) != null).collect(Collectors.toMap(getId, i -> i));
        Map<Object, T> updateBefore = oldData.stream().filter(i -> update.get(getId.apply(i)) != null).collect(Collectors.toMap(getId, i -> i));
        Map<Object, T> delete = oldData.stream().filter(i -> update.get(getId.apply(i)) == null).collect(Collectors.toMap(getCommon, i -> i));
        //2.比较添加的和删除的属性是否一样
        List<Object> deleteKey = new ArrayList<>();
        for (Object key : add.keySet()) {
            if (delete.get(key) != null) {
                T up = add.get(key);
                L id = getId.apply(delete.get(key));
                setId.accept(up, id);
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
        result.put(Tag.ADD, new ArrayList<T>(add.values()));
        result.put(Tag.UPDATE, new ArrayList<T>(update.values()));
        result.put(Tag.UPDATE_BEFORE, new ArrayList<T>(updateBefore.values()));
        result.put(Tag.DELETE, new ArrayList<T>(delete.values()));
        return result;
    }

    public enum Tag {
        ADD, UPDATE, UPDATE_BEFORE, DELETE
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

        System.out.println(CompareDataUtil.compareData(newDate, oldDate, DateDTO::getId, DateDTO::setId, DateDTO::getName));
        System.out.println(CompareDataUtil.compareData(newDate, oldDate, DateDTO::getId));
    }

}

class DateDTO implements Serializable {

    private static final long serialVersionUID = -5143554877607218116L;
    private Integer id;
    private String name;
    private String remark;

    public DateDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "DateDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
    
}
