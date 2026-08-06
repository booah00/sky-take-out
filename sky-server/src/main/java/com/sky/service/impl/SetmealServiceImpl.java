package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
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

import static com.sky.constant.MessageConstant.SETMEAL_ENABLE_FAILED;
import static com.sky.constant.MessageConstant.SETMEAL_ON_SALE;
import static com.sky.constant.StatusConstant.ENABLE;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐及套餐菜品关系
     *
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
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
        //批量向套餐菜品关系表插入数据（setmealDishMapper.insertBatch）
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     *
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
     *
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 循环查询套餐，若状态为起售则抛出DeletionNotAllowedException(SETMEAL_ON_SALE)
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == ENABLE) {
                throw new DeletionNotAllowedException(SETMEAL_ON_SALE);
            }
        }
        // setmealMapper.deleteByIds删除套餐
        setmealMapper.deleteByIds(ids);
        //setmealDishMapper.deleteBySetmealIds删除套餐菜品关系
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐及关联菜品
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //setmealMapper.getById查询套餐
        Setmeal setmeal = setmealMapper.getById(id);
        //setmealDishMapper.getBySetmealId查询关联菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        //合并封装为SetmealVO返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐及套餐菜品关系
     *
     * @param setmealDTO
     */
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        // setmealMapper.update修改套餐基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        //setmealDishMapper.deleteBySetmealId删除原有关系
        Long id = setmealDTO.getId();
        setmealDishMapper.deleteBySetmealId(id);
        //为套餐菜品关系数据补充setmealId并重新批量插入(setmealDishMapper.insertBatch)
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for(SetmealDish setmealDishe : setmealDishes){
            setmealDishe.setSetmealId(id);
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐起售停售
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //若为起售(status=1)，校验套餐内菜品是否全部处于启售状态，否则抛出SetmealEnableFailedException
        if (status == 1) {
            List<Long> dishIds = setmealDishMapper.getDishIdsBySetmealId(id);
            for (Long dishId : dishIds) {
                if (dishMapper.getById(dishId).getStatus() != 1) {
                    throw new SetmealEnableFailedException(SETMEAL_ENABLE_FAILED);
                }
            }
        }
        // setmealMapper.update修改套餐状态
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(id);
        setmealMapper.update(setmeal);
    }
}