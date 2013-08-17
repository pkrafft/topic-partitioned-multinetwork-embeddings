
output_summaries_folder=../output-summaries

Rscript scripts/plotCoherence.R $output_summaries_folder/
Rscript scripts/plotPrecisionRecall.R $output_summaries_folder/
