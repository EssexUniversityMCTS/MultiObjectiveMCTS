find ./ -name "*.pdf" | while read FILE; do
pdfcrop "$FILE"
done;
