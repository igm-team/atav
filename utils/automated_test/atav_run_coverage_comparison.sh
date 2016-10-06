. atav_config_paths.sh

atav_beta.sh --coverage-comparison --sample $ATAV_TEST_PATH/ped --gene-boundary $ATAV_TEST_PATH/gene_boundaries --min-coverage 10 --out $ATAV_OUTPUT_PATH/new/coverage_comparison

atav.sh --coverage-comparison --sample $ATAV_TEST_PATH/ped --gene-boundary $ATAV_TEST_PATH/gene_boundaries --min-coverage 10 --out $ATAV_OUTPUT_PATH/old/coverage_comparison
