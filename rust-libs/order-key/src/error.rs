#[derive(Debug, thiserror::Error)]
pub enum OrderKeyError {
    #[error("left key {l:?} must be strictly less than right key {r:?}")]
    LhsLess { l: Vec<u32>, r: Vec<u32> },
    #[error("lhs {l:?} is equal to rhs {r:?}")]
    Equal { l: Vec<u32>, r: Vec<u32> },
    #[error("lhs {l:?} is min")]
    Min { l: Vec<u32> },
    #[error("{l:?} is invalid")]
    Invalid { l: Vec<u32> },
    #[error("cannot rebalance {count} order keys")]
    RebalanceCountTooLarge { count: usize },
}
