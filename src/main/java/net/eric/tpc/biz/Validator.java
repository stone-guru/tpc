package net.eric.tpc.biz;

import net.eric.tpc.base.ActionStatus;

/**
 * 业务实体检查器。
 * 
 * <p>
 * 该对象用来将业务规则独立表达。利于随着业务规则的变化，软件结构本身保持稳定。
 * </p>
 * 
 * @author Eric.G
 *
 * @param <T>
 */
public interface Validator<T> {
    ActionStatus validate(T e);
}
