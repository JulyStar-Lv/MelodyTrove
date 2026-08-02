use std::cmp::Ordering;

use order_key::OrderKey;

use crate::error::BResult;

#[derive(Debug, Clone, uniffi::Record)]
pub struct OrderKeyValue {
    pub segments: Vec<u32>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct OrderKeyBatch {
    pub keys: Vec<OrderKeyValue>,
}

#[uniffi::export]
pub fn order_key_default() -> OrderKeyValue {
    OrderKey::default().into()
}

#[uniffi::export]
pub fn order_key_less(value: OrderKeyValue) -> BResult<OrderKeyValue> {
    let value = parse(value)?;
    Ok(OrderKey::less(&value)?.into())
}

#[uniffi::export]
pub fn order_key_greater(value: OrderKeyValue) -> BResult<OrderKeyValue> {
    let value = parse(value)?;
    Ok(OrderKey::greater(&value).into())
}

#[uniffi::export]
pub fn order_key_between(left: OrderKeyValue, right: OrderKeyValue) -> BResult<OrderKeyValue> {
    let left = parse(left)?;
    let right = parse(right)?;
    Ok(OrderKey::between(&left, &right)?.into())
}

#[uniffi::export]
pub fn order_key_compare(left: OrderKeyValue, right: OrderKeyValue) -> BResult<i8> {
    Ok(match parse(left)?.cmp(&parse(right)?) {
        Ordering::Less => -1,
        Ordering::Equal => 0,
        Ordering::Greater => 1,
    })
}

#[uniffi::export]
pub fn order_key_is_strictly_increasing(values: OrderKeyBatch) -> BResult<bool> {
    let values = parse_batch(values)?;
    Ok(OrderKey::is_strictly_increasing(&values))
}

#[uniffi::export]
pub fn order_key_needs_rebalance(values: OrderKeyBatch) -> bool {
    OrderKey::needs_rebalance(
        &values
            .keys
            .into_iter()
            .map(|value| value.segments)
            .collect::<Vec<_>>(),
    )
}

#[uniffi::export]
pub fn order_key_rebalance(count: u64) -> BResult<OrderKeyBatch> {
    let count = usize::try_from(count)
        .map_err(|_| order_key::OrderKeyError::RebalanceCountTooLarge { count: usize::MAX })?;
    Ok(OrderKeyBatch {
        keys: OrderKey::rebalance(count)?
            .into_iter()
            .map(Into::into)
            .collect(),
    })
}

fn parse(value: OrderKeyValue) -> BResult<OrderKey> {
    Ok(OrderKey::try_from_raw(value.segments)?)
}

fn parse_batch(values: OrderKeyBatch) -> BResult<Vec<OrderKey>> {
    values.keys.into_iter().map(parse).collect()
}

impl From<OrderKey> for OrderKeyValue {
    fn from(value: OrderKey) -> Self {
        Self {
            segments: value.into_raw(),
        }
    }
}
