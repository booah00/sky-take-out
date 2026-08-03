package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐及套餐菜品关系
     * @param setmealDTO
     */
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 向套餐表插入一条数据（setmealMapper.insert）
        log.info("新增套餐：{}", setmealDTO);
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStatus(1);
        setmealMapper.insert(setmeal);
        // 为套餐菜品关系数据补充setmealId
        Long id = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for(SetmealDish setmealDish : setmealDishes){
            setmealDish.setSetmealId(id);
        }
        //批量向套餐菜品关系表插入数据（setmealDishMapper.insertBatch）
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // PageHelper.startPage分页
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // setmealMapper.pageQuery查询（联表分类表获取categoryName）
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        // 封装PageResult返回
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐及关联关系
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // TODO 1.循环查询套餐，若状态为起售则抛出DeletionNotAllowedException(SETMEAL_ON_SALE)
        // TODO 2.setmealMapper.deleteByIds删除套餐
        // TODO 3.setmealDishMapper.deleteBySetmealIds删除套餐菜品关系
    }

    /**
     * 根据id查询套餐及关联菜品
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // TODO 1.setmealMapper.getById查询套餐
        // TODO 2.setmealDishMapper.getBySetmealId查询关联菜品
        // TODO 3.合并封装为SetmealVO返回
        return null;
    }

    /**
     * 修改套餐及套餐菜品关系
     * @param setmealDTO
     */
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        // TODO 1.setmealMapper.update修改套餐基本信息
        // TODO 2.setmealDishMapper.deleteBySetmealId删除原有关系
        // TODO 3.为套餐菜品关系数据补充setmealId并重新批量插入(setmealDishMapper.insertBatch)
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // TODO 1.若为起售(status=1)，校验套餐内菜品是否全部处于启售状态，否则抛出SetmealEnableFailedException(SETMEAL_ENABLE_FAILED)
        // TODO 2.setmealMapper.update修改套餐状态
    }
}