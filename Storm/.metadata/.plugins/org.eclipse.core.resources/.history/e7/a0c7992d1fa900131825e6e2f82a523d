library(rjson)
library(Storm)

storm = Storm$new();

storm$lambda = function(s) {
  t = s$tuple;
  word = paste(t$input[1],"###");
  t$output[1] = word;
  s$emit(t);
};

storm$run();
